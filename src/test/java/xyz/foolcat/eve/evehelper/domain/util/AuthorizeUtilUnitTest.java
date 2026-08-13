package xyz.foolcat.eve.evehelper.domain.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.SysUserDetails;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 权限工具单元测试。
 * <p>
 * 重点验证 authorize 为 fail-closed：未认证/匿名/主体无法识别一律拒绝，
 * 不再静默降级为硬编码用户 1；系统内部调用改由 authorizeInternal 显式声明。
 *
 * @author Leojan
 * date 2026-08-07
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("权限工具单元测试")
class AuthorizeUtilUnitTest {

    private static final Integer CHARACTER_ID = 2112818290;

    // 手动构造 + @Mock 隔离:避免 @MockBean EveAccountService 被后台定时任务调用,
    // 污染 verify never 断言(@SpringBootTest 加载上下文后定时任务会触发)
    @Mock
    EveAccountService eveAccountService;

    private AuthorizeUtil authorizeUtil;

    @BeforeEach
    void setUp() {
        authorizeUtil = new AuthorizeUtil(eveAccountService);
    }

    @AfterEach
    void tearDown() {
        // 安全上下文是 ThreadLocal，必须清理否则污染同线程后续测试
        SecurityContextHolder.clearContext();
    }

    /**
     * 模拟生产环境的 JWT 认证：principal 是 userId claim（Long），
     * 与 JwtAuthorizationTokenFilter 放入安全上下文的类型一致
     */
    private void loginAsJwt(long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("USER"))));
    }

    @Test
    @DisplayName("未认证（无安全上下文）-> 拒绝，且不得查库")
    void unauthenticated_throwsAndNeverQueries() {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorize(CHARACTER_ID));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        // 关键：不得再以硬编码用户 1 的身份查库
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }

    @Test
    @DisplayName("匿名主体（字符串）-> 拒绝，且不得查库")
    void anonymousPrincipal_throwsAndNeverQueries() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorize(CHARACTER_ID));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }

    @Test
    @DisplayName("主体为 0 -> 拒绝")
    void zeroPrincipal_throws() {
        loginAsJwt(0L);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorize(CHARACTER_ID));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }

    @Test
    @DisplayName("主体为负数 -> 拒绝（原 -1 语义不再放行）")
    void negativePrincipal_throws() {
        loginAsJwt(-1L);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorize(CHARACTER_ID));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }

    @Test
    @DisplayName("JWT 主体（Long）-> 按该 userId 查询，不得错用其他身份")
    void jwtPrincipal_queriesWithOwnUserId() {
        loginAsJwt(7L);
        EveAccount expected = new EveAccount();
        when(eveAccountService.getAccountOne(7, CHARACTER_ID)).thenReturn(expected);

        assertSame(expected, authorizeUtil.authorize(CHARACTER_ID));

        verify(eveAccountService).getAccountOne(7, CHARACTER_ID);
    }

    @Test
    @DisplayName("表单登录主体（SysUserDetails）-> 按其 id 查询")
    void sysUserPrincipal_queriesWithOwnUserId() {
        SysUser user = new SysUser();
        user.setId(9);
        SysUserDetails principal = new SysUserDetails(user, List.of(new SimpleGrantedAuthority("USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("USER"))));
        EveAccount expected = new EveAccount();
        when(eveAccountService.getAccountOne(9, CHARACTER_ID)).thenReturn(expected);

        assertSame(expected, authorizeUtil.authorize(CHARACTER_ID));

        verify(eveAccountService).getAccountOne(9, CHARACTER_ID);
    }

    @Test
    @DisplayName("表单登录主体 id 为 null -> 拒绝（fail-closed）")
    void sysUserPrincipalNullId_throws() {
        SysUser user = new SysUser();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("USER"))));

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorize(CHARACTER_ID));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }

    @Test
    @DisplayName("内部调用：显式声明身份，不读安全上下文（未认证下也能执行）")
    void authorizeInternal_ignoresSecurityContext() {
        EveAccount expected = new EveAccount();
        when(eveAccountService.getAccountOne(GlobalConstants.SYSTEM_USER_ID, CHARACTER_ID))
                .thenReturn(expected);

        // 未设置任何安全上下文，模拟定时任务/WebSocket 场景
        assertSame(expected, authorizeUtil.authorizeInternal(
                GlobalConstants.SYSTEM_USER_ID, CHARACTER_ID));

        verify(eveAccountService).getAccountOne(GlobalConstants.SYSTEM_USER_ID, CHARACTER_ID);
    }

    @Test
    @DisplayName("内部调用：userId 为空 -> 抛参数错误，不得回落到默认身份")
    void authorizeInternal_nullUserId_throwsParamError() {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorizeInternal(null, CHARACTER_ID));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }

    @Test
    @DisplayName("内部调用：userId 为 0 -> 抛参数错误")
    void authorizeInternal_zeroUserId_throwsParamError() {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorizeInternal(0, CHARACTER_ID));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }

    @Test
    @DisplayName("内部调用：userId 为负 -> 抛参数错误（不得把 -1 当内部身份）")
    void authorizeInternal_negativeUserId_throwsParamError() {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorizeInternal(-1, CHARACTER_ID));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }

    @Test
    @DisplayName("内部调用：处于已认证的请求上下文中 -> 拒绝（内部通道不得被请求路径误用）")
    void authorizeInternal_inAuthenticatedRequestContext_throws() {
        // 已存在可识别的登录主体，说明这是请求路径，不该走内部通道
        loginAsJwt(7L);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, CHARACTER_ID));

        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountOne(any(), any());
    }
}
