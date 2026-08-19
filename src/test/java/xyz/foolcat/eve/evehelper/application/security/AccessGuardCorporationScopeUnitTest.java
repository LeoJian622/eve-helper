package xyz.foolcat.eve.evehelper.application.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AccessGuard.corporationScope 军团维度读过滤口令单元测试（纯 Mockito，无 DB）。
 *
 * <p>构造注入 {@link ResourceOwnershipPolicy} mock（不触发 Spring 上下文，避免 DB/Redis 依赖）。
 * 契约：ROOT → 返回 null（看全量，不过滤）；已认证普通用户 → 返回当前 userId 的 Long 值（对齐
 * user_id BIGINT）；未认证或主体无法识别(UserUtil.getUserId()=-1) → 抛 ACCESS_UNAUTHORIZED
 * （fail-closed，禁止空值穿透到下游查询）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccessGuard.corporationScope 单元测试")
class AccessGuardCorporationScopeUnitTest {

    @Mock
    private ResourceOwnershipPolicy resourceOwnershipPolicy;

    private AccessGuard accessGuard;

    @BeforeEach
    void setUp() {
        // 构造注入，纯 Mockito 无 Spring 上下文
        accessGuard = new AccessGuard(resourceOwnershipPolicy);
    }

    @AfterEach
    void tearDown() {
        // 安全上下文是 ThreadLocal，必须清理否则污染同线程后续测试
        SecurityContextHolder.clearContext();
    }

    private void loginAsJwt(long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("ROOT：返回 null，不过滤（看全量）")
    void root_returnsNull() {
        loginAsJwt(5L, GlobalConstants.ROOT_ROLE_CODE);

        assertNull(accessGuard.corporationScope("军团读"));
    }

    @Test
    @DisplayName("携带 ADMIN 权限但令牌未认证、主体不可识别：不得泄露 ROOT 豁免，抛 ACCESS_UNAUTHORIZED（fail-closed）")
    void rootUnauthenticated_throws() {
        // 未认证的主体不可识别（字符串 → UserUtil.getUserId()=-1），即便带 ADMIN 权限
        // 也不豁免 — 仿 requireOwnership_unauthenticatedAdminToken_throws 的 "anonymousUser" 口径
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("anonymousUser", null,
                        List.of(new SimpleGrantedAuthority(GlobalConstants.ROOT_ROLE_CODE)));
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.corporationScope("军团读"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
    }

    @Test
    @DisplayName("已认证普通用户：返回当前 userId 的 Long 值")
    void authenticatedUser_returnsLong() {
        loginAsJwt(5L, "USER");

        Long scope = accessGuard.corporationScope("军团读");

        assertEquals(5L, scope);
    }

    @Test
    @DisplayName("未认证（无上下文）：抛 ACCESS_UNAUTHORIZED")
    void unauthenticated_throws() {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.corporationScope("军团读"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
    }

    @Test
    @DisplayName("主体无法识别（GetId()=-1）：抛 ACCESS_UNAUTHORIZED")
    void notIdentifiablePrincipal_throws() {
        // 匿名访问 principal 为字符串 → UserUtil.getUserId()=-1
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> accessGuard.corporationScope("军团读"));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
    }
}