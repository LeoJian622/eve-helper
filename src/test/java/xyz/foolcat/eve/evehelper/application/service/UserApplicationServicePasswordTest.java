package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.foolcat.eve.evehelper.application.assembler.system.EveAccountAssembler;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.application.dto.request.ChangePasswordRequest;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.port.appws.AppNotificationPushGateway;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRepository;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenService;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 改密应用服务单元测试（T041，4 用例）。
 * <p>
 * 覆盖纪元写入、全会话撤销、WS 关闭、旧 access token 纪元拒绝联动。
 *
 * @author Leojian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("改密应用服务测试(T041)")
class UserApplicationServicePasswordTest {

    @Mock private EveAccountService eveAccountService;
    @Mock private EsiGateway esiGateway;
    @Mock private EveAccountAssembler eveAccountAssembler;
    @Mock private SysUserService sysUserService;
    @Mock private SysUserAssembler userAssembler;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Executor esiAuthStatusExecutor;
    @Mock private AccessGuard accessGuard;
    @Mock private SysUserRepository sysUserRepository;
    @Mock private CacheGateway cacheGateway;
    @Mock private TokenService tokenService;
    @Mock private AppNotificationPushGateway pushGateway;

    private UserApplicationService service;

    private static final Integer USER_ID = 42;

    private ChangePasswordRequest req;
    private SysUser user;

    @BeforeEach
    void setUp() {
        service = new UserApplicationService(
                eveAccountService, esiGateway, eveAccountAssembler,
                sysUserService, userAssembler, passwordEncoder,
                esiAuthStatusExecutor, accessGuard,
                sysUserRepository, cacheGateway, tokenService, pushGateway
        );

        req = new ChangePasswordRequest();
        req.setOldPassword("OldPass123!");
        req.setNewPassword("NewPass456!");

        user = new SysUser();
        user.setId(USER_ID);
        user.setPassword("$2a$10$storedhashsynthetic");
    }

    // ── AC: 纪元写入（7 天 TTL） ──

    @Test
    @DisplayName("changePassword_epochWrittenWith7dTtl: 纪元键值与 TTL 正确")
    void changePassword_epochWrittenWith7dTtl() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);
            when(sysUserRepository.queryById(USER_ID)).thenReturn(user);
            when(passwordEncoder.matches("OldPass123!", "$2a$10$storedhashsynthetic")).thenReturn(true);
            when(passwordEncoder.encode("NewPass456!")).thenReturn("encoded-new");
            when(cacheGateway.sMembers("refresh_user:42")).thenReturn(Set.of("sid-1", "sid-2"));
            when(tokenService.revokeRefreshTokenBySession(anyString())).thenReturn(true);

            service.changePassword(req);

            // 纪元写入
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
            ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);
            verify(cacheGateway).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture(), unitCaptor.capture());

            assertThat(keyCaptor.getValue()).isEqualTo("auth:user:revoke_epoch:42");
            assertThat(valueCaptor.getValue()).isInstanceOf(Long.class);
            long epoch = (Long) valueCaptor.getValue();
            assertThat(Math.abs(epoch - System.currentTimeMillis())).isLessThanOrEqualTo(5000);
            assertThat(ttlCaptor.getValue()).isEqualTo(7L);
            assertThat(unitCaptor.getValue()).isEqualTo(TimeUnit.DAYS);
        }
    }

    // ── AC: 逐 sid 撤销 + 标记 ──

    @Test
    @DisplayName("changePassword_allIndexedSessionsRevoked: 逐 sid 撤销并标记")
    void changePassword_allIndexedSessionsRevoked() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);
            when(sysUserRepository.queryById(USER_ID)).thenReturn(user);
            when(passwordEncoder.matches("OldPass123!", "$2a$10$storedhashsynthetic")).thenReturn(true);
            when(passwordEncoder.encode("NewPass456!")).thenReturn("encoded-new");
            when(cacheGateway.sMembers("refresh_user:42")).thenReturn(Set.of("s1", "s2", "s3"));
            when(tokenService.revokeRefreshTokenBySession(anyString())).thenReturn(true);

            service.changePassword(req);

            verify(tokenService).revokeRefreshTokenBySession("s1");
            verify(tokenService).revokeRefreshTokenBySession("s2");
            verify(tokenService).revokeRefreshTokenBySession("s3");
            verify(tokenService).markSessionRevoked("s1");
            verify(tokenService).markSessionRevoked("s2");
            verify(tokenService).markSessionRevoked("s3");
        }
    }

    // ── AC: WS 会话关闭 ──

    @Test
    @DisplayName("changePassword_wsSessionsClosed: markSessionRevoked 先于 closeUserSessions")
    void changePassword_wsSessionsClosed() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);
            when(sysUserRepository.queryById(USER_ID)).thenReturn(user);
            when(passwordEncoder.matches("OldPass123!", "$2a$10$storedhashsynthetic")).thenReturn(true);
            when(passwordEncoder.encode("NewPass456!")).thenReturn("encoded-new");
            when(cacheGateway.sMembers("refresh_user:42")).thenReturn(Set.of("sid-1"));
            when(tokenService.revokeRefreshTokenBySession(anyString())).thenReturn(true);

            service.changePassword(req);

            // InOrder 验证：markSessionRevoked 先于 closeUserSessions
            var inOrder = inOrder(tokenService, pushGateway);
            inOrder.verify(tokenService).markSessionRevoked(anyString());
            inOrder.verify(pushGateway).closeUserSessions(USER_ID);
            verify(pushGateway).closeUserSessions(USER_ID);
        }
    }

    // ── AC: 旧 access token 纪元拒绝联动（T006 过滤器侧由回归测试覆盖） ──

    @Test
    @DisplayName("changePassword_oldAccessTokenRejectedAfterEpoch: 纪元键与 T006 REVOKE_EPOCH_PREFIX 逐字一致")
    void changePassword_oldAccessTokenRejectedAfterEpoch() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);
            when(sysUserRepository.queryById(USER_ID)).thenReturn(user);
            when(passwordEncoder.matches("OldPass123!", "$2a$10$storedhashsynthetic")).thenReturn(true);
            when(passwordEncoder.encode("NewPass456!")).thenReturn("encoded-new");
            when(cacheGateway.sMembers("refresh_user:42")).thenReturn(Set.of());

            service.changePassword(req);

            // 纪元键与 JwtAuthorizationTokenFilter.REVOKE_EPOCH_PREFIX 逐字一致（"auth:user:revoke_epoch:"）
            // 过滤器侧拒绝由 JwtAuthorizationTokenFilterEpochTest / EpochRevocationTest 回归覆盖
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
            verify(cacheGateway).set(keyCaptor.capture(), valueCaptor.capture(), eq(7L), eq(TimeUnit.DAYS));

            assertThat(keyCaptor.getValue()).isEqualTo("auth:user:revoke_epoch:" + USER_ID);
            assertThat(valueCaptor.getValue()).isInstanceOf(Long.class);
            long epoch = (Long) valueCaptor.getValue();
            assertThat(Math.abs(epoch - System.currentTimeMillis())).isLessThanOrEqualTo(5000);
        }
    }
}
