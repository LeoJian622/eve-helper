package xyz.foolcat.eve.evehelper.infrastructure.gateway.esi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterOnlineInfo;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterPublicInfo;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiAccessException;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiCacheTtlExtractor;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCache;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCacheProperties;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.ForceRefreshLimiter;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CharacterPublicInfoResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.OnlineStatusResponse;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 角色面板网关查询单元测试(T009)。
 * <p>
 * 纯单元测试,不依赖 Spring 上下文;mock SDK API、缓存组件与限流器,
 * 验证 publicInfo/online 的缓存段归属、缓存命中跳过 SDK、强刷限流与异常转换。
 *
 * @author Leojan
 * date 2026-11-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色面板网关查询(T009)")
class EsiApiServiceCharacterPanelTest {

    @Mock private CacheGateway cacheGateway;
    @Mock private EveAccountService eveAccountService;
    @Mock private AuthorizeOAuth authorizeOAuth;
    @Mock private CharacterApi characterApi;
    @Mock private UniverseApi universeApi;
    @Mock private AssetsApi assetsApi;
    @Mock private CorporationApi corporationApi;
    @Mock private IndustryApi industryApi;
    @Mock private WalletApi walletApi;
    @Mock private LocationApi locationApi;
    @Mock private EsiAssetsConverter esiAssetsConverter;
    @Mock private EsiIndustryJobConverter esiIndustryJobConverter;
    @Mock private EsiInvTypesConverter esiInvTypesConverter;
    @Mock private EsiMiningDetailConverter esiMiningDetailConverter;
    @Mock private EsiStructureConverter esiStructureConverter;
    @Mock private EsiUniverseNameConverter esiUniverseNameConverter;
    @Mock private EsiWalletJournalConverter esiWalletJournalConverter;
    @Mock private EsiWalletTransactionConverter esiWalletTransactionConverter;
    @Mock private EsiResponseCache esiResponseCache;
    @Mock private EsiCacheTtlExtractor esiCacheTtlExtractor;
    @Mock private ForceRefreshLimiter forceRefreshLimiter;
    @Mock private EsiResponseCacheProperties esiResponseCacheProperties;

    private EsiApiService esiApiService;

    private static final Integer CHARACTER_ID = 95465499;
    private static final Integer USER_ID = 100;
    private static final String ACCESS_TOKEN = "Bearer test-token";
    private static final String DATASOURCE = "serenity";

    @BeforeEach
    void setUp() {
        esiApiService = new EsiApiService(
                cacheGateway, eveAccountService, authorizeOAuth,
                characterApi, universeApi, assetsApi, corporationApi, industryApi, walletApi, locationApi,
                esiAssetsConverter, esiIndustryJobConverter, esiInvTypesConverter,
                esiMiningDetailConverter, esiStructureConverter,
                esiUniverseNameConverter, esiWalletJournalConverter, esiWalletTransactionConverter,
                esiResponseCache, esiCacheTtlExtractor, forceRefreshLimiter,
                esiResponseCacheProperties, new ObjectMapper()
        );
        lenient().when(esiResponseCacheProperties.getDatasource()).thenReturn(DATASOURCE);
        lenient().when(esiResponseCacheProperties.getForceRefreshWindowMinutes()).thenReturn(60);
        lenient().when(esiResponseCacheProperties.getForceRefreshLimit()).thenReturn(5);
    }

    // ── publicInfo ──

    @Test
    @DisplayName("publicInfo 使用 pub 缓存段(userId=null),SDK 返回公开信息")
    void publicInfo_usesPubCacheSegment() {
        // Arrange: 缓存未命中,SDK 返回公开信息
        String expectedKey = "esi:resp:" + DATASOURCE + ":pub:/characters/" + CHARACTER_ID + "/:0";
        when(esiResponseCache.cacheKey(eq(DATASOURCE), isNull(), eq("/characters/" + CHARACTER_ID + "/"), eq(0)))
                .thenReturn(expectedKey);
        when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
        when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

        CharacterPublicInfoResponse sdkResponse = new CharacterPublicInfoResponse();
        sdkResponse.setName("Test Character");
        sdkResponse.setCorporationId(1001);
        sdkResponse.setAllianceId(2001);
        sdkResponse.setBirthday(OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        sdkResponse.setSecurityStatus(1.5);

        when(characterApi.queryCharacter(eq(CHARACTER_ID), eq(DATASOURCE)))
                .thenReturn(Mono.just(sdkResponse));

        // Act
        CharacterPublicInfo result = esiApiService.queryCharacterPublicInfo(CHARACTER_ID);

        // Assert: 验证 pub 段(userId=null)被使用
        verify(esiResponseCache).cacheKey(eq(DATASOURCE), isNull(), eq("/characters/" + CHARACTER_ID + "/"), eq(0));
        assertThat(result).isNotNull();
        assertThat(result.characterId()).isEqualTo(CHARACTER_ID);
        assertThat(result.name()).isEqualTo("Test Character");
        assertThat(result.corporationId()).isEqualTo(1001);
        assertThat(result.allianceId()).isEqualTo(2001);
        verify(characterApi).queryCharacter(CHARACTER_ID, DATASOURCE);
    }

    // ── online ──

    @Test
    @DisplayName("online 缓存命中 → 直接返回,不调用 SDK")
    void online_cacheHit_noSdkCall() {
        // Arrange: mock UserUtil 返回有效 userId,缓存命中
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/online/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/online/"), eq(0)))
                    .thenReturn(expectedKey);

            CharacterOnlineInfo cachedInfo = new CharacterOnlineInfo(true,
                    OffsetDateTime.parse("2026-11-12T10:00:00Z"),
                    OffsetDateTime.parse("2026-11-12T09:00:00Z"));
            when(esiResponseCache.getList(eq(expectedKey), any()))
                    .thenReturn(List.of(cachedInfo));

            // Act
            CharacterOnlineInfo result = esiApiService.queryCharacterOnline(CHARACTER_ID, ACCESS_TOKEN);

            // Assert: 缓存命中,SDK 未被调用
            assertThat(result).isNotNull();
            assertThat(result.online()).isTrue();
            assertThat(result.lastLogin()).isEqualTo(OffsetDateTime.parse("2026-11-12T10:00:00Z"));
            verify(locationApi, never()).queryCharacterOnline(anyInt(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("online forceRefresh → 经 ForceRefreshLimiter 限流后真调")
    void online_forceRefresh_bypassesCacheViaLimiter() {
        // Arrange: mock UserUtil,forceRefresh=true,限流器允许
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/online/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/online/"), eq(0)))
                    .thenReturn(expectedKey);
            when(forceRefreshLimiter.tryAcquire(eq(expectedKey), any(), eq(5))).thenReturn(true);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            when(locationApi.queryCharacterOnline(eq(CHARACTER_ID), eq(DATASOURCE), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(createOnlineResponse(false)));

            // Act
            CharacterOnlineInfo result = esiApiService.queryCharacterOnline(CHARACTER_ID, ACCESS_TOKEN, true);

            // Assert: 限流器被调用,SDK 被调用
            verify(forceRefreshLimiter).tryAcquire(eq(expectedKey), any(), eq(5));
            verify(locationApi).queryCharacterOnline(CHARACTER_ID, DATASOURCE, ACCESS_TOKEN);
            assertThat(result).isNotNull();
            assertThat(result.online()).isFalse();
        }
    }

    // ── 异常转换 ──

    @Test
    @DisplayName("SDK EsiException → 转换为 EsiAccessException")
    void sdkError_convertedToAccessException() {
        // Arrange: SDK 抛出 EsiException(403 权限不足)
        when(esiResponseCache.cacheKey(anyString(), any(), anyString(), anyInt()))
                .thenReturn("dummy-key");
        when(esiResponseCache.getList(anyString(), any())).thenReturn(null);
        when(esiResponseCache.tryClaim(anyString(), any())).thenReturn(true);

        when(characterApi.queryCharacter(eq(CHARACTER_ID), eq(DATASOURCE)))
                .thenThrow(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW));

        // Act & Assert
        assertThatThrownBy(() -> esiApiService.queryCharacterPublicInfo(CHARACTER_ID))
                .isInstanceOf(EsiAccessException.class)
                .hasMessageContaining(EsiAccessException.MSG_PERMISSION_LOW);
    }

    // ── helpers ──

    private OnlineStatusResponse createOnlineResponse(boolean online) {
        OnlineStatusResponse resp = new OnlineStatusResponse();
        resp.setOnline(online);
        resp.setLastLogin(OffsetDateTime.parse("2026-11-12T10:00:00Z"));
        resp.setLastLogout(OffsetDateTime.parse("2026-11-12T09:00:00Z"));
        return resp;
    }
}
