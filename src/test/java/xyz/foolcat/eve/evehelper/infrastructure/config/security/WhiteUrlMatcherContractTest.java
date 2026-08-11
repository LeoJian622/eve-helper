package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * {@code WhiteUrlMatcher} 与 {@link RbacAuthorizationManager} 白名单判定一致性契约测试
 * (007 CRITICAL-1,T004;对应 plan v3 §3.2 AC#4)。
 *
 * <p><b>背景</b>:007 将让 {@code JwtAuthorizationTokenFilter} 在验签失败时依据白名单
 * 决定「匿名放行」还是「直写 401」。白名单判定目前内联在
 * {@code RbacAuthorizationManager:67-73}(精确字符串匹配 {@code method + ":" + URI}),
 * 过滤器不可见。若两处各写一份判定逻辑,语义漂移将导致「RBAC 放行、过滤器拦截」的
 * 401 死循环(CRITICAL-1)。本契约测试固化唯一语义,防漂移:</p>
 * <ol>
 *   <li>对同一组 {@code method + URI} 输入,两者判定结果一致;</li>
 *   <li>{@code POST:/auth/tokens} 在白名单中时命中;</li>
 *   <li>大小写/尾斜杠不匹配 —— 精确字符串语义,非 Ant 通配。</li>
 * </ol>
 *
 * <p><b>已知有意分歧</b>:manager 的 OPTIONS 短路({@code :56-58})<b>不在</b>
 * {@code WhiteUrlMatcher} 内(CORS 预检不带 Authorization 头,走过滤器
 * 「非 JWT 不处理」分支),见 plan v3 §3.2 遗留风险说明与 round3 §1.3。</p>
 *
 * <p>TDD 状态:T004 = RED({@code WhiteUrlMatcher} 尚不存在,本测试编译失败);
 * T005 实现后转 GREEN;T006 重构 manager 委托本组件后本测试仍须 GREEN。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("白名单匹配契约测试(007 CRITICAL-1)")
class WhiteUrlMatcherContractTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private EveHelperSecurityConfig config;

    private WhiteUrlMatcher whiteUrlMatcher;

    private RbacAuthorizationManager rbacAuthorizationManager;

    @BeforeEach
    void setUp() {
        config = new EveHelperSecurityConfig();
        config.setWhiteUrlList(new ArrayList<>(List.of("POST:/user", "POST:/auth/tokens")));

        whiteUrlMatcher = new WhiteUrlMatcher(config);
        // T006 后 manager 委托 WhiteUrlMatcher 判定白名单(构造注入)
        rbacAuthorizationManager = new RbacAuthorizationManager(redisTemplate, whiteUrlMatcher);

        // 非白名单请求会继续走 Redis RBAC 规则查询;返回空规则集 → 一律拒绝。
        // lenient:纯 matcher 用例不会触碰该桩。
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(hashOperations.entries(GlobalConstants.URL_PERM_ROLES_KEY))
                .thenReturn(Collections.emptyMap());
    }

    // ------------------------------------------------------------------
    // ① 一致性契约:同一组 method+URI 输入,matcher 与 manager 判定一致
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1} → 白名单命中 = {2}")
    @CsvSource({
            // 命中项
            "POST, /user,            true",
            "POST, /auth/tokens,     true",
            // 方法不同 → 不命中(精确匹配语义)
            "GET,    /user,          false",
            "GET,    /auth/tokens,   false",
            "DELETE, /auth/tokens,   false",
            // 路径不同 → 不命中
            "POST, /user/123,        false",
            "POST, /auth,            false",
            "GET,  /swagger-ui.html, false",
    })
    @DisplayName("契约:WhiteUrlMatcher 与 RbacAuthorizationManager 对同一输入判定一致")
    void contract_matcherAndManagerAgreeOnSameInput(String method, String uri, boolean expectedWhiteListed) {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);

        // Act
        boolean matcherResult = whiteUrlMatcher.isWhiteListed(request);
        boolean managerResult = managerAllows(request);

        // Assert:两者一致,且等于预期
        assertEquals(expectedWhiteListed, matcherResult,
                "WhiteUrlMatcher 判定与预期不符: " + method + " " + uri);
        assertEquals(matcherResult, managerResult,
                "语义漂移!manager 与 matcher 判定不一致: " + method + " " + uri);
    }

    // ------------------------------------------------------------------
    // ② POST:/auth/tokens 在白名单时命中;移除后不再命中(配置驱动)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST:/auth/tokens 在白名单中时应命中;从列表移除后不再命中")
    void isWhiteListed_refreshEndpoint_hitOnlyWhenConfigured() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/tokens");

        // Act & Assert:在白名单中 → 命中
        assertTrue(whiteUrlMatcher.isWhiteListed(request),
                "POST:/auth/tokens 已配置在白名单中,应命中");

        // Act & Assert:从白名单移除后 → 不再命中
        config.setWhiteUrlList(new ArrayList<>(List.of("POST:/user")));
        assertFalse(whiteUrlMatcher.isWhiteListed(request),
                "POST:/auth/tokens 移出白名单后不应命中");
    }

    // ------------------------------------------------------------------
    // ③ 精确字符串语义固化:大小写/尾斜杠/前后缀均不匹配
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "变体 {0} {1} 不应命中")
    @CsvSource({
            "POST, /auth/tokens/",   // 尾斜杠
            "POST, /auth//tokens",   // 重复斜杠
            "post, /auth/tokens",    // 方法小写
            "POST, /AUTH/TOKENS",    // 路径大写
            "POST, ' /auth/tokens'", // 路径前导空格(引号防止 CsvSource 修剪空白)
    })
    @DisplayName("精确匹配语义:大小写/尾斜杠/空白变体均不命中")
    void isWhiteListed_exactMatchSemantics_variantsDoNotMatch(String method, String uri) {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);

        // Act & Assert
        assertFalse(whiteUrlMatcher.isWhiteListed(request),
                "精确字符串匹配下该变体不应命中: '" + method + ":" + uri + "'");
    }

    // ------------------------------------------------------------------
    // 边界:null/空列表安全(当前 manager 内联实现对 null 会 NPE,
    // matcher 作为单一事实来源必须自保 —— T005 AC)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("whiteUrlList 为 null 时应返回 false 而非抛异常")
    void isWhiteListed_nullList_returnsFalseWithoutException() {
        // Arrange
        config.setWhiteUrlList(null);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user");

        // Act & Assert
        assertFalse(whiteUrlMatcher.isWhiteListed(request), "null 列表应视为无白名单");
    }

    @Test
    @DisplayName("whiteUrlList 为空列表时应返回 false")
    void isWhiteListed_emptyList_returnsFalse() {
        // Arrange
        config.setWhiteUrlList(new ArrayList<>());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user");

        // Act & Assert
        assertFalse(whiteUrlMatcher.isWhiteListed(request), "空列表应不命中任何路径");
    }

    // ------------------------------------------------------------------
    // 语义边界:OPTIONS 短路属于 manager,不属于 matcher(有意分歧)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("OPTIONS 请求:matcher 不命中(短路在 manager 侧,有意分歧)")
    void isWhiteListed_optionsRequest_matcherDoesNotShortCircuit() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/tokens");

        // Act & Assert:matcher 不做 OPTIONS 短路 —— 该语义留在 manager:56-58
        assertFalse(whiteUrlMatcher.isWhiteListed(request),
                "OPTIONS 短路不属于 WhiteUrlMatcher;若此处变为 true,说明语义被错误合并");
        assertTrue(managerAllows(request),
                "manager 的 OPTIONS 短路应保持放行(既有行为不变)");
    }

    /**
     * 以匿名身份调用 manager 的白名单/RBAC 判定。
     * 白名单命中时在触碰 Redis 前即返回;未命中则走空规则集 → 拒绝。
     */
    private boolean managerAllows(MockHttpServletRequest request) {
        Authentication anonymous = mock(Authentication.class);
        // doReturn 绕开 getAuthorities() 通配返回类型的泛型 capture 问题
        lenient().doReturn(Collections.<GrantedAuthority>emptyList()).when(anonymous).getAuthorities();
        lenient().when(anonymous.isAuthenticated()).thenReturn(false);

        AuthorizationDecision decision = rbacAuthorizationManager.check(
                () -> anonymous, new RequestAuthorizationContext(request));
        return decision.isGranted();
    }
}
