package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.application.assembler.system.EveAccountAssembler;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.application.dto.UserAccountDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * UserApplicationService.queryAccountListWithAuthStatus 集成测试(@SpringBootTest + @MockBean)。
 * <p>
 * 覆盖多角色并行判定中的异常隔离与空列表降级,对应 003-esi-auth-status 任务 T012。
 * 注入真实 bean,queryAccountListWithAuthStatus 经 future.get() 阻塞等待每个角色判定,
 * 因此真实异步执行器下断言仍具确定性。
 *
 * @author Leojan
 * date 2026-08-07
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("用户应用服务-授权状态编排单元测试 - 异常隔离与空列表")
class UserApplicationServiceTest {

    @MockBean
    EveAccountService eveAccountService;

    @MockBean
    EsiGateway esiApiService;

    @MockBean
    EveAccountAssembler eveAccountAssembler;

    @MockBean
    SysUserService sysUserService;

    @MockBean
    SysUserAssembler userAssembler;

    // 不 @MockBean PasswordEncoder:覆盖 SecurityConfig.passwordEncoder 会破坏 securityFilterChain 上下文;
    // 本类无 register 测试,不需 mock,真实 bean 由 @Autowired UserApplicationService 内部使用
    @Autowired
    private UserApplicationService userApplicationService;

    @AfterEach
    void tearDown() {
        // 安全上下文是 ThreadLocal,必须清理否则污染同线程后续测试
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
     * 模拟 JWT 认证:principal 是 userId claim(Long),
     * 与 JwtAuthorizationTokenFilter 放入安全上下文的类型一致。
     */
    private void loginAsJwt(long userId, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    @DisplayName("3 角色中 1 个 ESI 异常被隔离为 UNKNOWN,其余 2 个状态正确,接口仍成功返回 3 条")
    void threeAccounts_oneThrows_isolatedAsUnknown() {
        loginAsJwt(1L, "USER");

        EveAccount a1 = account(1, "rt1");
        EveAccount a2 = account(2, "rt2");
        EveAccount a3 = account(3, "rt3");
        when(eveAccountService.getAccountList(1)).thenReturn(List.of(a1, a2, a3));

        UserAccountDTO d1 = dto(1);
        UserAccountDTO d2 = dto(2);
        UserAccountDTO d3 = dto(3);
        when(eveAccountAssembler.domain2UserAccountTO(anyList())).thenReturn(List.of(d1, d2, d3));

        when(esiApiService.getAuthorizationStatus(a1)).thenReturn(EsiAuthStatus.AUTHORIZED);
        when(esiApiService.getAuthorizationStatus(a2)).thenThrow(new RuntimeException("ESI unavailable"));
        when(esiApiService.getAuthorizationStatus(a3)).thenReturn(EsiAuthStatus.EXPIRED);

        List<UserAccountDTO> result = userApplicationService.queryAccountListWithAuthStatus(1);

        assertEquals(3, result.size());
        assertEquals(EsiAuthStatus.AUTHORIZED, d1.getAuthStatus());
        assertEquals(EsiAuthStatus.UNKNOWN, d2.getAuthStatus());
        assertEquals(EsiAuthStatus.EXPIRED, d3.getAuthStatus());
    }

    @Test
    @DisplayName("空角色列表 -> 返回空列表")
    void emptyAccounts_returnsEmptyList() {
        loginAsJwt(1L, "USER");
        when(eveAccountService.getAccountList(1)).thenReturn(Collections.emptyList());

        List<UserAccountDTO> result = userApplicationService.queryAccountListWithAuthStatus(1);

        assertTrue(result.isEmpty());
    }
}
