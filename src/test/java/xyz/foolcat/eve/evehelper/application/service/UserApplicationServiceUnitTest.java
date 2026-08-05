package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.foolcat.eve.evehelper.application.assembler.system.EveAccountAssembler;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.application.dto.UserAccountDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
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
    EsiApiService esiApiService;

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
        userApplicationService = new UserApplicationService(
                eveAccountService, esiApiService, eveAccountAssembler,
                sysUserService, userAssembler, passwordEncoder, esiAuthStatusExecutor);
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

    @Test
    @DisplayName("无绑定角色 -> 返回空列表")
    void emptyAccounts_returnsEmpty() {
        when(eveAccountService.getAccountList(1)).thenReturn(Collections.emptyList());

        assertTrue(userApplicationService.queryAccountListWithAuthStatus(1).isEmpty());
    }

    @Test
    @DisplayName("多角色并行判定,各角色状态正确设置")
    void multipleAccounts_statusesSet() {
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
        when(eveAccountService.getAccountList(1)).thenReturn(null);

        assertTrue(userApplicationService.queryAccountListWithAuthStatus(1).isEmpty());
    }

    @Test
    @DisplayName("非本人且非 ROOT 访问 -> 抛 ACCESS_UNAUTHORIZED(IDOR 防御)")
    void otherUserAccess_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(999L);
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            assertThrows(EveHelperException.class,
                    () -> userApplicationService.queryAccountListWithAuthStatus(1));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("本人访问 -> 放行")
    void selfAccess_allowed() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(1L);
        when(eveAccountService.getAccountList(1)).thenReturn(Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            assertTrue(userApplicationService.queryAccountListWithAuthStatus(1).isEmpty());
        } finally {
            SecurityContextHolder.clearContext();
        }
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