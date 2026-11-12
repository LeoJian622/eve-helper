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
import xyz.foolcat.eve.evehelper.application.dto.response.AuthorizeUrlResponse;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.port.appws.AppNotificationPushGateway;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRepository;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenService;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 账号自助服务单元测试(T039, RED-only, 9 用例)。
 * <p>
 * 本测试为 RED 阶段产物,预期编译失败:
 * <ul>
 *   <li>ChangePasswordRequest 不存在(T041 产物)</li>
 *   <li>AuthorizeUrlResponse 不存在(T043 产物)</li>
 *   <li>ResultCode.RATE_LIMITED 不存在(T041 产物)</li>
 *   <li>UserApplicationService.changePassword() 不存在(T041 产物)</li>
 *   <li>CharacterApplicationService.unbindCharacter() 不存在(T042 产物)</li>
 *   <li>CharacterApplicationService.getAuthorizeUrl() 不存在(T043 产物)</li>
 *   <li>EsiGateway.buildAuthorizeUrl() 不存在(T043 产物)</li>
 *   <li>UserApplicationService 构造器 12 参 vs 既有 8 参(T041 扩展)</li>
 *   <li>CharacterApplicationService 构造器 4 参 vs 既有 1 参(T042/T043 扩展)</li>
 * </ul>
 *
 * @author Leojian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US6 账号自助服务测试")
class AccountSelfServiceTest {

    // ---------------------------------------------------------------
    //  Mocks — UserApplicationService 12 参
    // ---------------------------------------------------------------

    @Mock
    private EveAccountService eveAccountService;

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private EveAccountAssembler eveAccountAssembler;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysUserAssembler userAssembler;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Executor esiAuthStatusExecutor;

    @Mock
    private AccessGuard accessGuard;

    @Mock
    private SysUserRepository sysUserRepository;

    @Mock
    private CacheGateway cacheGateway;

    @Mock
    private TokenService tokenService;

    @Mock
    private AppNotificationPushGateway pushGateway;

    // ---------------------------------------------------------------
    //  Mocks — CharacterApplicationService 4 参(共用 esiGateway/cacheGateway)
    // ---------------------------------------------------------------

    @Mock
    private AuthorizeUtil authorizeUtil;

    @Mock
    private EveAccountRepository eveAccountRepository;

    // ---------------------------------------------------------------
    //  Services under test
    // ---------------------------------------------------------------

    private UserApplicationService userApplicationService;

    private CharacterApplicationService characterApplicationService;

    // ---------------------------------------------------------------
    //  Common fixtures
    // ---------------------------------------------------------------

    private static final Integer USER_ID = 42;

    private ChangePasswordRequest req;

    private SysUser user;

    private EveAccount account;

    @BeforeEach
    void setUp() {
        // UserApplicationService 12 参构造器(T041 扩展)
        userApplicationService = new UserApplicationService(
                eveAccountService,
                esiGateway,
                eveAccountAssembler,
                sysUserService,
                userAssembler,
                passwordEncoder,
                esiAuthStatusExecutor,
                accessGuard,
                sysUserRepository,
                cacheGateway,
                tokenService,
                pushGateway
        );

        // CharacterApplicationService 3 参构造器(IMP-1: CacheGateway 已移除)
        characterApplicationService = new CharacterApplicationService(
                esiGateway,
                authorizeUtil,
                eveAccountRepository
        );

        // Common fixtures
        req = new ChangePasswordRequest();
        req.setOldPassword("OldPass123!");
        req.setNewPassword("NewPass456!");

        user = new SysUser();
        user.setId(42);
        user.setPassword("$2a$10$storedhashsynthetic");

        account = new EveAccount();
        account.setUserId(42);
        account.setCharacterId(1001);
        account.setCorpId(500);
    }

    // =================================================================
    //  US6-1  修改密码 — 正确旧密码,成功并撤销所有会话
    // =================================================================

    @Test
    @DisplayName("changePassword 正确旧密码成功并撤销所有会话")
    void changePassword_correctOld_succeeds_andRevokesAllSessions() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // Arrange
            when(sysUserRepository.queryById(USER_ID)).thenReturn(user);
            when(passwordEncoder.matches("OldPass123!", "$2a$10$storedhashsynthetic")).thenReturn(true);
            when(passwordEncoder.encode("NewPass456!")).thenReturn("encoded-new");
            when(cacheGateway.sMembers("refresh_user:42")).thenReturn(Set.of("sid-1", "sid-2"));
            when(tokenService.revokeRefreshTokenBySession(anyString())).thenReturn(true);

            // Act
            userApplicationService.changePassword(req);

            // Assert — 密码更新
            ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
            verify(sysUserRepository).insertOrUpdateSelective(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(42);
            assertThat(captor.getValue().getPassword()).isEqualTo("encoded-new");

            // Assert — 纪元写入(7 天 TTL)
            verify(cacheGateway).set(eq("auth:user:revoke_epoch:42"), any(), eq(7L), eq(TimeUnit.DAYS));

            // Assert — 逐 sid 撤销 + 标记(无条件)
            verify(tokenService).revokeRefreshTokenBySession("sid-1");
            verify(tokenService).revokeRefreshTokenBySession("sid-2");
            verify(tokenService).markSessionRevoked("sid-1");
            verify(tokenService).markSessionRevoked("sid-2");

            // Assert — WS 关闭 + 计数器清理
            verify(pushGateway).closeUserSessions(USER_ID);
            verify(cacheGateway).delete("pwd:attempt:42");
        }
    }

    // =================================================================
    //  US6-2  修改密码 — 错误旧密码,拒绝
    // =================================================================

    @Test
    @DisplayName("changePassword 错误旧密码被拒绝并计数")
    void changePassword_wrongOld_rejected() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // Arrange
            when(sysUserRepository.queryById(USER_ID)).thenReturn(user);
            when(passwordEncoder.matches("OldPass123!", "$2a$10$storedhashsynthetic")).thenReturn(false);
            when(cacheGateway.increment("pwd:attempt:42")).thenReturn(1L);

            // Act + Assert
            assertThatThrownBy(() -> userApplicationService.changePassword(req))
                    .isInstanceOf(EveHelperException.class)
                    .extracting(e -> ((EveHelperException) e).getResultCode())
                    .isEqualTo(ResultCode.PARAM_ERROR);

            // Assert — 首次失败设置过期时间
            verify(cacheGateway).expire("pwd:attempt:42", 30, TimeUnit.MINUTES);

            // Assert — 不得更新密码或关闭会话
            verify(sysUserRepository, never()).insertOrUpdateSelective(any());
            verify(pushGateway, never()).closeUserSessions(any());
        }
    }

    // =================================================================
    //  修改密码 — 连续 5 次失败,限流
    // =================================================================

    @Test
    @DisplayName("changePassword 连续 5 次失败后限流")
    void changePassword_rateLimited_after5Failures() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // Arrange — 计数器已达 5
            when(cacheGateway.get("pwd:attempt:42")).thenReturn(5);

            // Act + Assert
            assertThatThrownBy(() -> userApplicationService.changePassword(req))
                    .isInstanceOf(EveHelperException.class)
                    .extracting(e -> ((EveHelperException) e).getResultCode())
                    .isEqualTo(ResultCode.RATE_LIMITED);

            // Assert — 不得查询用户或尝试匹配
            verify(sysUserRepository, never()).queryById(any());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(cacheGateway, never()).increment(anyString());
        }
    }

    // =================================================================
    //  修改密码 — 新旧密码相同,拒绝
    // =================================================================

    @Test
    @DisplayName("changePassword 新旧密码相同被拒绝且不计数")
    void changePassword_sameAsOld_rejected() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // Arrange — 新旧相同
            req.setNewPassword("OldPass123!");
            when(sysUserRepository.queryById(USER_ID)).thenReturn(user);
            when(passwordEncoder.matches("OldPass123!", "$2a$10$storedhashsynthetic")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> userApplicationService.changePassword(req))
                    .isInstanceOf(EveHelperException.class)
                    .extracting(e -> ((EveHelperException) e).getResultCode())
                    .isEqualTo(ResultCode.PARAM_ERROR);

            // Assert — 不得更新、计数、编码
            verify(sysUserRepository, never()).insertOrUpdateSelective(any());
            verify(cacheGateway, never()).increment(anyString());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // =================================================================
    //  US6-3  解绑角色 — 自己的角色,成功删除
    // =================================================================

    @Test
    @DisplayName("unbindCharacter 解绑自己的角色并删除绑定")
    void unbind_ownCharacter_deletesBinding() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // Arrange
            when(authorizeUtil.authorize(1001)).thenReturn(account);

            // Act
            characterApplicationService.unbindCharacter(1001);

            // Assert — 删除绑定 + 委托 EsiGateway 驱逐 token 缓存（IMP-1）
            verify(eveAccountRepository).deleteByUserIdAndCharacterId(42, 1001);
            verify(esiGateway).evictAccessTokenCache(42, 1001);
        }
    }

    // =================================================================
    //  US6-4  解绑角色 — 他人的角色,拒绝
    // =================================================================

    @Test
    @DisplayName("unbindCharacter 解绑他人角色被拒绝")
    void unbind_otherUsersCharacter_rejected() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // Arrange — 归属校验失败
            when(authorizeUtil.authorize(1001))
                    .thenThrow(new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED));

            // Act + Assert
            assertThatThrownBy(() -> characterApplicationService.unbindCharacter(1001))
                    .isInstanceOf(EveHelperException.class)
                    .extracting(e -> ((EveHelperException) e).getResultCode())
                    .isEqualTo(ResultCode.ACCESS_UNAUTHORIZED);

            // Assert — 不得删除绑定或清理缓存
            verify(eveAccountRepository, never()).deleteByUserIdAndCharacterId(any(), any());
            verify(cacheGateway, never()).delete(anyString());
        }
    }

    // =================================================================
    //  解绑角色 — 系统同步角色,拒绝
    // =================================================================

    @Test
    @DisplayName("unbindCharacter 解绑系统同步角色被拒绝")
    void unbind_systemCharacter_rejected() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // Arrange — 系统角色 ID = 2112818290
            EveAccount sysAccount = new EveAccount();
            sysAccount.setUserId(42);
            sysAccount.setCharacterId(2112818290);
            sysAccount.setCorpId(500);
            when(authorizeUtil.authorize(2112818290)).thenReturn(sysAccount);

            // Act + Assert
            assertThatThrownBy(() -> characterApplicationService.unbindCharacter(2112818290))
                    .isInstanceOf(EveHelperException.class)
                    .extracting(e -> ((EveHelperException) e).getResultCode())
                    .isEqualTo(ResultCode.PARAM_ERROR);

            // Assert — 不得删除绑定
            verify(eveAccountRepository, never()).deleteByUserIdAndCharacterId(any(), any());
        }
    }

    // =================================================================
    //  授权 URL — 返回含 scopes 的 URL
    // =================================================================

    @Test
    @DisplayName("getAuthorizeUrl 返回含 scopes 的授权 URL")
    void authUrl_returnsUrlWithScopes() {
        // Arrange
        String expectedUrl = "https://login.eveonline.com/v2/oauth/authorize?response_type=code&client_id=test&scope=esi-skills.read_skillqueue.v1";
        when(esiGateway.buildAuthorizeUrl("serenity")).thenReturn(expectedUrl);

        // Act
        AuthorizeUrlResponse response = characterApplicationService.getAuthorizeUrl("serenity");

        // Assert
        assertThat(response.authorizeUrl()).isEqualTo(expectedUrl);
        assertThat(response.state()).isEqualTo("eve-helper");
    }

    // =================================================================
    //  授权 URL — 服务器不匹配,友好错误
    // =================================================================

    @Test
    @DisplayName("getAuthorizeUrl 服务器不匹配返回友好错误")
    void authUrl_serverMismatch_friendlyError() {
        // Arrange — 端口抛 PARAM_ERROR
        when(esiGateway.buildAuthorizeUrl("tranquility"))
                .thenThrow(new EveHelperException(ResultCode.PARAM_ERROR));

        // Act + Assert
        assertThatThrownBy(() -> characterApplicationService.getAuthorizeUrl("tranquility"))
                .isInstanceOf(EveHelperException.class)
                .extracting(e -> ((EveHelperException) e).getResultCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
    }
}
