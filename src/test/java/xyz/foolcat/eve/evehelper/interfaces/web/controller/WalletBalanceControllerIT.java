package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.WalletDivisionBalance;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.RbacAuthorizationManager;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户钱包余额聚合端点集成测试（018）。
 *
 * <p>MockMvc 走完整过滤器链 + 控制器 + 应用服务。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("用户钱包余额聚合端点集成测试")
class WalletBalanceControllerIT {

    private static final int USER_ID = 777;
    private static final int CID_1 = 2112832425;
    private static final int CORP_A = 98454654;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RbacAuthorizationManager rbacAuthorizationManager;

    @MockitoBean
    EsiGateway esiGateway;

    @MockitoBean
    EveAccountRepository eveAccountRepository;

    /**
     * AuthorizeUtil 必须 mock:真实实现会按 (userId, characterId) 查 eve_account 表，
     * 测试库中不存在该组合时抛"账号不存在"，而该异常会被应用服务 fail-open 的 catch
     * 吞成余额 0.0（见 IMPORTANT-2），使断言失败且原因不显。
     * 本类是控制器层集成测试，关注路由/认证/序列化契约；归属校验本身由
     * AuthorizeUtil 单测与 CharacterPrivacyIT 覆盖。同时避免向远程共享测试库写入数据。
     */
    @MockitoBean
    AuthorizeUtil authorizeUtil;

    @BeforeEach
    void setUp() {
        // RBAC 默认放行
        lenient().when(rbacAuthorizationManager.check(any(), any()))
                .thenReturn(new org.springframework.security.authorization.AuthorizationDecision(true));
    }

    @AfterEach
    void tearDown() {
        // 清理安全上下文，避免测试间状态污染
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("未认证 → 403 + AUT00301(应用服务身份兜底)")
    void unauthenticated_returnsForbidden() throws Exception {
        SecurityContextHolder.clearContext();

        // 口径说明:本项目 ACCESS_UNAUTHORIZED 由 GlobalExceptionHandler 映射为 HTTP 403
        // (而非语义上更贴切的 401),该映射对全部端点生效,属既有约定。
        //
        // 另注:本用例 mock 了 RbacAuthorizationManager 无条件放行,且
        // JwtAuthorizationTokenFilter 对"无 Authorization 头"是放行进链,
        // 因此请求实际到达了控制器,最终由应用服务的身份自取(UserUtil 返回 -1)拦下。
        // 即本用例验证的是【应用服务的身份兜底】,而非认证过滤器链本身;
        // 真实环境下 RBAC 会更早拦截。断言错误码以区分"兜底生效"与"任意 403"。
        mockMvc.perform(get("/wallet/balance"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultCode.ACCESS_UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("正常查询 → 返回正确结构")
    void normalQuery_returnsCorrectStructure() throws Exception {
        mockAuthenticated();

        EveAccount account = new EveAccount();
        account.setCharacterId(CID_1);
        account.setCharacterName("TestChar");
        account.setCorpId(CORP_A);
        account.setCorpName("TestCorp");
        account.setUserId(USER_ID);

        when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(List.of(account));

        // 归属校验 mock:返回同一 account，使 fetchCharacterBalance 能取到 userId 换 token
        when(authorizeUtil.authorize(CID_1)).thenReturn(account);

        // stub ESI
        lenient().when(esiGateway.getAccessToken(eq(CID_1), anyInt())).thenReturn("Bearer token");
        when(esiGateway.queryCharacterWalletBalance(eq(CID_1), any())).thenReturn(1000000.0);
        when(esiGateway.queryCorporationWalletBalances(eq(CORP_A), any()))
                .thenReturn(List.of(new WalletDivisionBalance(1, 500000.0)));

        mockMvc.perform(get("/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.characters").isArray())
                .andExpect(jsonPath("$.data.characters[0].characterId").value(CID_1))
                .andExpect(jsonPath("$.data.characters[0].characterName").value("TestChar"))
                .andExpect(jsonPath("$.data.characters[0].balance").value(1000000.0))
                .andExpect(jsonPath("$.data.characters[0].status").value("OK"))
                .andExpect(jsonPath("$.data.corporations").isArray())
                .andExpect(jsonPath("$.data.corporations[0].corpId").value(CORP_A))
                .andExpect(jsonPath("$.data.corporations[0].corpName").value("TestCorp"))
                .andExpect(jsonPath("$.data.corporations[0].totalBalance").value(500000.0))
                // 018 IMPORTANT-2:成功路径 partial 为 false、失败人物列表为空
                .andExpect(jsonPath("$.data.corporations[0].partial").value(false))
                .andExpect(jsonPath("$.data.corporations[0].unavailableCharacterIds").isEmpty());
    }

    @Test
    @DisplayName("无绑定人物 → 返回空结构")
    void noAccounts_returnsEmptyStructure() throws Exception {
        mockAuthenticated();
        when(eveAccountRepository.getAccountList(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.characters").isEmpty())
                .andExpect(jsonPath("$.data.corporations").isEmpty());
    }
}
