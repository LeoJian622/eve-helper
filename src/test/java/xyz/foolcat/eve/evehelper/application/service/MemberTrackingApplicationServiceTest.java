package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MemberTrackingInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.MemberTrackingItem;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 团员追踪应用服务单元测试(T036, RED-only, 6 用例)。
 * <p>
 * RED 阶段:本测试文件引用尚未实现的 MemberTrackingApplicationService 与
 * MemberTrackingInfo(T038 产物),编译失败即 RED 证据。
 *
 * @author Leojian
 * @date 2026-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US5 团员追踪应用服务测试")
class MemberTrackingApplicationServiceTest {

    @Mock
    private EsiGateway esiGateway;

    @Mock
    private AuthorizeUtil authorizeUtil;

    private static final Integer CORP_ID = 500;
    private static final Integer CHARACTER_ID = 1001;
    private static final Integer USER_ID = 42;
    private static final String ACCESS_TOKEN = "Bearer t-track";

    private MemberTrackingApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MemberTrackingApplicationService(esiGateway, authorizeUtil);
    }

    // ---------------------------------------------------------------
    //  track — Director 角色正常返回成员列表
    // ---------------------------------------------------------------

    @Test
    @DisplayName("track Director 角色返回成员列表并验证网关调用")
    void track_directorRole_returnsMembers() throws Exception {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            EveAccount account = new EveAccount();
            account.setUserId(USER_ID);
            account.setCharacterId(CHARACTER_ID);
            account.setCorpId(CORP_ID);

            CharacterAccessTokenResult tokenResult =
                    new CharacterAccessTokenResult(ACCESS_TOKEN, CHARACTER_ID, 1140L);

            CharacterRolesInfo directorRoles =
                    new CharacterRolesInfo(List.of("Director"), List.of(), List.of());

            OffsetDateTime now = OffsetDateTime.now();
            MemberTrackingItem m1 = new MemberTrackingItem(2001, 60001L, 670, now.minusHours(1), now.minusHours(2));
            MemberTrackingItem m2 = new MemberTrackingItem(2002, 60002L, 671, now.minusHours(3), now.minusHours(1));

            when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
            when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID)).thenReturn(tokenResult);
            when(esiGateway.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN)).thenReturn(directorRoles);
            when(esiGateway.queryCorporationMemberTracking(CORP_ID, ACCESS_TOKEN, false))
                    .thenReturn(List.of(m1, m2));

            // Act
            MemberTrackingInfo result = service.track(CORP_ID, CHARACTER_ID, false);

            // Assert
            assertThat(result.members()).hasSize(2);
            assertThat(result.trackedAt()).isNotNull();
            verify(esiGateway).queryCorporationMemberTracking(CORP_ID, ACCESS_TOKEN, false);
        }
    }

    // ---------------------------------------------------------------
    //  track — 非 Director 角色被拒绝
    // ---------------------------------------------------------------

    @Test
    @DisplayName("track 非 Director 角色抛出权限不足异常")
    void track_nonDirector_rejectedWithReason() throws Exception {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            EveAccount account = new EveAccount();
            account.setUserId(USER_ID);
            account.setCharacterId(CHARACTER_ID);
            account.setCorpId(CORP_ID);

            CharacterAccessTokenResult tokenResult =
                    new CharacterAccessTokenResult(ACCESS_TOKEN, CHARACTER_ID, 1140L);

            CharacterRolesInfo accountantRoles =
                    new CharacterRolesInfo(List.of(), List.of(), List.of("Accountant"));

            when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
            when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID)).thenReturn(tokenResult);
            when(esiGateway.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN)).thenReturn(accountantRoles);

            // Act & Assert
            assertThatThrownBy(() -> service.track(CORP_ID, CHARACTER_ID, false))
                    .isInstanceOf(EsiAccessException.class);

            verify(esiGateway, never()).queryCorporationMemberTracking(eq(CORP_ID), eq(ACCESS_TOKEN), eq(false));
        }
    }

    // ---------------------------------------------------------------
    //  track — online 派生:logon 在 logoff 之后 → online=true
    // ---------------------------------------------------------------

    @Test
    @DisplayName("track online 派生:logon 在 logoff 之后及 logoff null 均为 true")
    void track_onlineDerived_logonAfterLogoff() throws Exception {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            EveAccount account = new EveAccount();
            account.setUserId(USER_ID);
            account.setCharacterId(CHARACTER_ID);
            account.setCorpId(CORP_ID);

            CharacterAccessTokenResult tokenResult =
                    new CharacterAccessTokenResult(ACCESS_TOKEN, CHARACTER_ID, 1140L);

            CharacterRolesInfo directorRoles =
                    new CharacterRolesInfo(List.of("Director"), List.of(), List.of());

            OffsetDateTime now = OffsetDateTime.now();
            // member 1:logon after logoff → online true
            MemberTrackingItem m1 = new MemberTrackingItem(2001, 60001L, 670, now.minusHours(1), now.minusHours(2));
            // member 2:logoff null → online true
            MemberTrackingItem m2 = new MemberTrackingItem(2002, 60002L, 671, now.minusHours(1), null);

            when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
            when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID)).thenReturn(tokenResult);
            when(esiGateway.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN)).thenReturn(directorRoles);
            when(esiGateway.queryCorporationMemberTracking(CORP_ID, ACCESS_TOKEN, false))
                    .thenReturn(List.of(m1, m2));

            // Act
            MemberTrackingInfo result = service.track(CORP_ID, CHARACTER_ID, false);

            // Assert
            assertThat(result.members()).hasSize(2);
            assertThat(result.members().get(0).online()).isTrue();
            assertThat(result.members().get(1).online()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    //  track — offline 派生:logoff 在 logon 之后 → online=false
    // ---------------------------------------------------------------

    @Test
    @DisplayName("track offline 派生:logoff 在 logon 之后为 false")
    void track_offlineDerived_logoffAfterLogon() throws Exception {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            EveAccount account = new EveAccount();
            account.setUserId(USER_ID);
            account.setCharacterId(CHARACTER_ID);
            account.setCorpId(CORP_ID);

            CharacterAccessTokenResult tokenResult =
                    new CharacterAccessTokenResult(ACCESS_TOKEN, CHARACTER_ID, 1140L);

            CharacterRolesInfo directorRoles =
                    new CharacterRolesInfo(List.of("Director"), List.of(), List.of());

            OffsetDateTime now = OffsetDateTime.now();
            // logoff after logon → offline
            MemberTrackingItem m1 = new MemberTrackingItem(2001, 60001L, 670, now.minusHours(3), now.minusHours(1));

            when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
            when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID)).thenReturn(tokenResult);
            when(esiGateway.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN)).thenReturn(directorRoles);
            when(esiGateway.queryCorporationMemberTracking(CORP_ID, ACCESS_TOKEN, false))
                    .thenReturn(List.of(m1));

            // Act
            MemberTrackingInfo result = service.track(CORP_ID, CHARACTER_ID, false);

            // Assert
            assertThat(result.members()).hasSize(1);
            assertThat(result.members().get(0).online()).isFalse();
        }
    }

    // ---------------------------------------------------------------
    //  track — 他人角色被拒绝
    // ---------------------------------------------------------------

    @Test
    @DisplayName("track 他人角色抛出统一越权异常")
    void track_otherUsersCharacter_rejected() throws Exception {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            when(authorizeUtil.authorize(CHARACTER_ID))
                    .thenThrow(new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED));

            // Act & Assert
            assertThatThrownBy(() -> service.track(CORP_ID, CHARACTER_ID, false))
                    .isInstanceOf(EveHelperException.class)
                    .satisfies(ex -> assertThat(((EveHelperException) ex).getResultCode())
                            .isEqualTo(ResultCode.ACCESS_UNAUTHORIZED));

            verify(esiGateway, never()).queryCorporationMemberTracking(eq(CORP_ID), eq(ACCESS_TOKEN), eq(false));
        }
    }

    // ---------------------------------------------------------------
    //  track — 缓存键按 userId 隔离(spec US5-3)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("track token 按当前用户获取,验证 userId 段隔离")
    void track_cacheKeyContainsUserId() throws Exception {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            EveAccount account = new EveAccount();
            account.setUserId(USER_ID);
            account.setCharacterId(CHARACTER_ID);
            account.setCorpId(CORP_ID);

            CharacterAccessTokenResult tokenResult =
                    new CharacterAccessTokenResult(ACCESS_TOKEN, CHARACTER_ID, 1140L);

            CharacterRolesInfo directorRoles =
                    new CharacterRolesInfo(List.of("Director"), List.of(), List.of());

            OffsetDateTime now = OffsetDateTime.now();
            MemberTrackingItem m1 = new MemberTrackingItem(2001, 60001L, 670, now.minusHours(1), now.minusHours(2));

            when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);
            when(esiGateway.getAccessTokenWithExpiry(CHARACTER_ID, USER_ID)).thenReturn(tokenResult);
            when(esiGateway.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN)).thenReturn(directorRoles);
            when(esiGateway.queryCorporationMemberTracking(CORP_ID, ACCESS_TOKEN, false))
                    .thenReturn(List.of(m1));

            // Act
            service.track(CORP_ID, CHARACTER_ID, false);

            // Assert — getAccessTokenWithExpiry 以 (characterId, userId) 调用,保证缓存键 userId 段隔离
            verify(esiGateway).getAccessTokenWithExpiry(CHARACTER_ID, USER_ID);

            // queryCorporationMemberTracking 收到的 token == "Bearer t-track"
            ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
            verify(esiGateway).queryCorporationMemberTracking(eq(CORP_ID), tokenCaptor.capture(), eq(false));
            assertThat(tokenCaptor.getValue()).isEqualTo(ACCESS_TOKEN);
        }
    }

    // ── MIN-E: 跨军团本地守卫 ──

    @Test
    @DisplayName("MIN-E: path corpId ≠ account.getCorpId() → 本地 PARAM_ERROR，不调 ESI")
    void track_crossCorpId_localParamError() throws Exception {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            EveAccount account = new EveAccount();
            account.setUserId(USER_ID);
            account.setCharacterId(CHARACTER_ID);
            account.setCorpId(CORP_ID); // account 属于 corp 500

            when(authorizeUtil.authorize(CHARACTER_ID)).thenReturn(account);

            Integer differentCorpId = 999; // path 传入不同 corpId

            assertThatThrownBy(() -> service.track(differentCorpId, CHARACTER_ID, false))
                    .isInstanceOf(EveHelperException.class)
                    .satisfies(e -> assertThat(((EveHelperException) e).getResultCode())
                            .isEqualTo(ResultCode.PARAM_ERROR));

            // 本地守卫先于 ESI 直查：ESI 零调用
            verify(esiGateway, never()).getAccessTokenWithExpiry(eq(CHARACTER_ID), eq(USER_ID));
            verify(esiGateway, never()).queryCorporationMemberTracking(eq(differentCorpId), eq(ACCESS_TOKEN), eq(false));
        }
    }
}
