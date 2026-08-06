package xyz.foolcat.eve.evehelper.application.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.foolcat.eve.evehelper.application.assembler.system.EveAccountAssembler;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.application.dto.UserAccountDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户应用服务-授权状态编排单元测试(Mockito,同步执行器保证确定性)。
 *
 * @author Leojan
 * date 2026-08-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户应用服务-授权状态编排单元测试")
class UserApplicationServiceUnitTest {

    @Mock
    EveAccountService eveAccountService;

    @Mock
    EsiGateway esiApiService;

    @Mock
    EveAccountAssembler eveAccountAssembler;

    @Mock
    SysUserService sysUserService;

    @Mock
    SysUserAssembler userAssembler;

    @Mock
    PasswordEncoder passwordEncoder;

    /**
     * 同步执行器:任务在调用线程直接执行,保证测试确定性(并行逻辑仍走 CompletableFuture)。
     */
    private final Executor esiAuthStatusExecutor = Runnable::run;

    private UserApplicationService userApplicationService;

    @BeforeEach
    void setUp() {
        // 使用真实 AccessGuard，保证守卫逻辑本身被测试覆盖而非被 mock 掉
        AccessGuard accessGuard = new AccessGuard(new ResourceOwnershipPolicy(eveAccountService));
        userApplicationService = new UserApplicationService(
                eveAccountService, esiApiService, eveAccountAssembler,
                sysUserService, userAssembler, passwordEncoder, esiAuthStatusExecutor, accessGuard);
    }

    @AfterEach
    void tearDown() {
        // 安全上下文是 ThreadLocal，必须清理否则污染同线程后续测试
        SecurityContextHolder.clearContext();
    }

    private EveAccount account(int id, String refreshToken) {
        EveAccount a = new EveAccount();
        a.setCharacterId(id);
        a.setRefreshToken(refreshToken);
        return a;
    }

    private UserAccountDTO dto(int id) {
        UserAccountDTO d = new UserAccountDTO();
        d.setCharacterId(id);
        return d;
    }

    /**
     * 模拟生产环境的 JWT 认证:principal 是 userId claim(Long),
     * 与 JwtAuthorizationTokenFilter 放入安全上下文的类型一致
     */
    private void loginAsJwt(long userId, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
    }

    /**
     * 模拟表单登录(formLogin)的认证:principal 是 SysUser 实体,
     * 覆盖 UserUtil 的 SysUser 分支
     */
    private void loginAsSysUser(Integer userId, String... roles) {
        SysUser user = new SysUser();
        user.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null,
                        Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    @DisplayName("无绑定角色 -> 返回空列表")
    void emptyAccounts_returnsEmpty() {
        loginAsJwt(1L, "USER");
        when(eveAccountService.getAccountList(1)).thenReturn(Collections.emptyList());

        assertTrue(userApplicationService.queryAccountListWithAuthStatus(1).isEmpty());
    }

    @Test
    @DisplayName("多角色并行判定,各角色状态正确设置")
    void multipleAccounts_statusesSet() {
        loginAsJwt(1L, "USER");
        EveAccount a1 = account(1, "rt1");
        EveAccount a2 = account(2, "rt2");
        EveAccount a3 = account(3, null);
        when(eveAccountService.getAccountList(1)).thenReturn(List.of(a1, a2, a3));
        UserAccountDTO d1 = dto(1);
        UserAccountDTO d2 = dto(2);
        UserAccountDTO d3 = dto(3);
        when(eveAccountAssembler.domain2UserAccountTO(anyList())).thenReturn(List.of(d1, d2, d3));
        when(esiApiService.getAuthorizationStatus(a1)).thenReturn(EsiAuthStatus.AUTHORIZED);
        when(esiApiService.getAuthorizationStatus(a2)).thenReturn(EsiAuthStatus.EXPIRED);
        when(esiApiService.getAuthorizationStatus(a3)).thenReturn(EsiAuthStatus.NOT_AUTHORIZED);

        List<UserAccountDTO> result = userApplicationService.queryAccountListWithAuthStatus(1);

        assertEquals(3, result.size());
        assertEquals(EsiAuthStatus.AUTHORIZED, d1.getAuthStatus());
        assertEquals(EsiAuthStatus.EXPIRED, d2.getAuthStatus());
        assertEquals(EsiAuthStatus.NOT_AUTHORIZED, d3.getAuthStatus());
    }

    @Test
    @DisplayName("单角色判定抛异常 -> 隔离为 UNKNOWN,其他角色不受影响")
    void oneThrows_isolatedAsUnknown() {
        loginAsJwt(1L, "USER");
        EveAccount a1 = account(1, "rt1");
        EveAccount a2 = account(2, "rt2");
        when(eveAccountService.getAccountList(1)).thenReturn(List.of(a1, a2));
        UserAccountDTO d1 = dto(1);
        UserAccountDTO d2 = dto(2);
        when(eveAccountAssembler.domain2UserAccountTO(anyList())).thenReturn(List.of(d1, d2));
        when(esiApiService.getAuthorizationStatus(a1)).thenReturn(EsiAuthStatus.AUTHORIZED);
        when(esiApiService.getAuthorizationStatus(a2)).thenThrow(new RuntimeException("boom"));

        List<UserAccountDTO> result = userApplicationService.queryAccountListWithAuthStatus(1);

        assertEquals(2, result.size());
        assertEquals(EsiAuthStatus.AUTHORIZED, d1.getAuthStatus());
        assertEquals(EsiAuthStatus.UNKNOWN, d2.getAuthStatus());
    }

    @Test
    @DisplayName("getAccountList 返回 null -> 返回空列表(防御)")
    void nullAccounts_returnsEmpty() {
        loginAsJwt(1L, "USER");
        when(eveAccountService.getAccountList(1)).thenReturn(null);

        assertTrue(userApplicationService.queryAccountListWithAuthStatus(1).isEmpty());
    }

    @Test
    @DisplayName("非本人且非 ROOT 访问 -> 抛 ACCESS_UNAUTHORIZED(IDOR 防御)")
    void otherUserAccess_throws() {
        loginAsJwt(999L, "USER");

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(1));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        // 拒绝必须发生在读库之前,否则守卫下沉的重构会静默失效
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("未认证(无安全上下文)访问 -> 拒绝(fail-closed)")
    void unauthenticated_throws() {
        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(1));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("匿名主体(字符串)访问 -> 拒绝(fail-closed)")
    void anonymousPrincipal_throws() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(1));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("主体为 0 -> 拒绝(fail-closed)")
    void zeroPrincipal_throws() {
        loginAsJwt(0L, "USER");

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(1));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("userId 为 null -> 抛 PARAM_ERROR(不依赖 equals(null) 的巧合)")
    void nullUserId_throwsParamError() {
        loginAsJwt(1L, "USER");

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(null));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("ROOT 传 null userId -> 抛 PARAM_ERROR,不得穿透到读库")
    void rootWithNullUserId_throwsParamError() {
        loginAsJwt(999L, GlobalConstants.ROOT_ROLE_CODE);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(null));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("携带 ADMIN 权限但未认证的令牌 -> 拒绝(ROOT 豁免也须 fail-closed)")
    void unauthenticatedTokenWithAdminAuthority_throws() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("anonymousUser", null,
                        List.of(new SimpleGrantedAuthority(GlobalConstants.ROOT_ROLE_CODE)));
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(1));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("表单登录主体(SysUser)本人访问 -> 放行")
    void sysUserPrincipalSelf_allowed() {
        loginAsSysUser(1, "USER");
        when(eveAccountService.getAccountList(1)).thenReturn(Collections.emptyList());

        assertTrue(userApplicationService.queryAccountListWithAuthStatus(1).isEmpty());
    }

    @Test
    @DisplayName("表单登录主体(SysUser)访问他人 -> 拒绝")
    void sysUserPrincipalOther_throws() {
        loginAsSysUser(999, "USER");

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(1));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("表单登录主体 id 为 null -> 拒绝(fail-closed)")
    void sysUserPrincipalNullId_throws() {
        loginAsSysUser(null, "USER");

        EveHelperException ex = assertThrows(EveHelperException.class,
                () -> userApplicationService.queryAccountListWithAuthStatus(1));
        assertEquals(ResultCode.ACCESS_UNAUTHORIZED.getCode(), ex.getResultCode().getCode());
        verify(eveAccountService, never()).getAccountList(any());
    }

    @Test
    @DisplayName("ROOT 角色访问他人 -> 放行")
    void rootAccessOther_allowed() {
        loginAsJwt(999L, GlobalConstants.ROOT_ROLE_CODE);
        when(eveAccountService.getAccountList(1)).thenReturn(Collections.emptyList());

        assertTrue(userApplicationService.queryAccountListWithAuthStatus(1).isEmpty());
    }

    @Test
    @DisplayName("本人访问 -> 放行")
    void selfAccess_allowed() {
        loginAsJwt(1L, "USER");
        when(eveAccountService.getAccountList(1)).thenReturn(Collections.emptyList());

        assertTrue(userApplicationService.queryAccountListWithAuthStatus(1).isEmpty());
    }

    @Test
    @DisplayName("注册:加密密码并入库")
    void register_encodesAndInserts() {
        UserDTO user = new UserDTO();
        user.setUsername("alice");
        user.setPassword("raw");
        SysUser sysUser = new SysUser();
        sysUser.setPassword("raw");
        when(userAssembler.userDto2SysUser(user)).thenReturn(sysUser);
        when(passwordEncoder.encode("raw")).thenReturn("encoded");

        userApplicationService.register(user);

        verify(sysUserService).insert(sysUser);
        assertEquals("encoded", sysUser.getPassword());
    }

    @Test
    @DisplayName("注册:DTO 转换失败 -> 抛 PARAM_ERROR")
    void register_nullConversion_throws() {
        UserDTO user = new UserDTO();
        when(userAssembler.userDto2SysUser(user)).thenReturn(null);

        assertThrows(EveHelperException.class, () -> userApplicationService.register(user));
    }
}