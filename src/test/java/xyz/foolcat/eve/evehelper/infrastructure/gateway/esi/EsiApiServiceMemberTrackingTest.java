package xyz.foolcat.eve.evehelper.infrastructure.gateway.esi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.vo.MemberTrackingItem;
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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.MemberTrackingResponse;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 团员追踪网关查询单元测试(T037)。
 * <p>
 * 纯单元测试,不依赖 Spring 上下文;mock SDK API、缓存组件与限流器,
 * 验证 membertracking 五字段映射、userId 隔离缓存与 403 权限转换。
 *
 * @author Leojan
 * date 2026-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("团员追踪网关查询(T037)")
class EsiApiServiceMemberTrackingTest {

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
    @Mock private SkillApi skillApi;
    @Mock private MailApi mailApi;
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

    private static final Integer CORPORATION_ID = 98000001;
    private static final String ACCESS_TOKEN = "Bearer test-token";
    private static final String DATASOURCE = "serenity";
    private static final Duration EXPECTED_TTL = Duration.ofSeconds(300);

    @BeforeEach
    void setUp() {
        esiApiService = new EsiApiService(
                cacheGateway, eveAccountService, authorizeOAuth,
                characterApi, universeApi, assetsApi, corporationApi, industryApi, walletApi, locationApi, skillApi, mailApi,
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

    // ── AC: 五字段映射 ──

    @Test
    @DisplayName("tracking_mapsAllFields: SDK 返回两条记录→VO 五字段逐一直传")
    void tracking_mapsAllFields() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(42);

            String expectedKey = "esi:resp:" + DATASOURCE + ":42:/corporations/" + CORPORATION_ID + "/membertracking/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(42), eq("/corporations/" + CORPORATION_ID + "/membertracking/"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");

            OffsetDateTime logon1 = OffsetDateTime.parse("2026-11-01T10:00:00Z");
            OffsetDateTime logoff1 = OffsetDateTime.parse("2026-11-01T18:00:00Z");
            OffsetDateTime logon2 = OffsetDateTime.parse("2026-11-02T09:30:00Z");
            OffsetDateTime logoff2 = OffsetDateTime.parse("2026-11-02T17:30:00Z");

            MemberTrackingResponse resp1 = new MemberTrackingResponse();
            resp1.setCharacterId(10001);
            resp1.setLocationId(60003760L);
            resp1.setShipTypeId(587);
            resp1.setLogonDate(logon1);
            resp1.setLogoffDate(logoff1);
            // baseId / startDate 设置但不映射(YAGNI)
            resp1.setBaseId(999);
            resp1.setStartDate(OffsetDateTime.parse("2020-01-01T00:00:00Z"));

            MemberTrackingResponse resp2 = new MemberTrackingResponse();
            resp2.setCharacterId(10002);
            resp2.setLocationId(60005686L);
            resp2.setShipTypeId(670);
            resp2.setLogonDate(logon2);
            resp2.setLogoffDate(logoff2);

            when(corporationApi.queryCorporationMembersTrackingResponse(eq(CORPORATION_ID), eq(DATASOURCE), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(List.of(resp1, resp2))));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            List<MemberTrackingItem> result = esiApiService.queryCorporationMemberTracking(CORPORATION_ID, ACCESS_TOKEN);

            // Assert
            assertThat(result).hasSize(2);

            assertThat(result.get(0).characterId()).isEqualTo(10001);
            assertThat(result.get(0).locationId()).isEqualTo(60003760L);
            assertThat(result.get(0).shipTypeId()).isEqualTo(587);
            assertThat(result.get(0).logonDate()).isEqualTo(logon1);
            assertThat(result.get(0).logoffDate()).isEqualTo(logoff1);

            assertThat(result.get(1).characterId()).isEqualTo(10002);
            assertThat(result.get(1).locationId()).isEqualTo(60005686L);
            assertThat(result.get(1).shipTypeId()).isEqualTo(670);
            assertThat(result.get(1).logonDate()).isEqualTo(logon2);
            assertThat(result.get(1).logoffDate()).isEqualTo(logoff2);

            verify(corporationApi).queryCorporationMembersTrackingResponse(CORPORATION_ID, DATASOURCE, ACCESS_TOKEN);
        }
    }

    // ── AC: userId 缓存隔离 ──

    @Test
    @DisplayName("tracking_cacheIsolatedByUserId: 不同 userId 产生不同缓存键")
    void tracking_cacheIsolatedByUserId() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            // 第一次: userId=42
            userUtilMock.when(UserUtil::getUserId).thenReturn(42);

            String key42 = "esi:resp:" + DATASOURCE + ":42:/corporations/" + CORPORATION_ID + "/membertracking/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(42), eq("/corporations/" + CORPORATION_ID + "/membertracking/"), eq(0)))
                    .thenReturn(key42);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            when(esiResponseCache.getList(eq(key42), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(key42), any())).thenReturn(true);

            MemberTrackingResponse resp = new MemberTrackingResponse();
            resp.setCharacterId(10001);
            resp.setLocationId(60003760L);
            resp.setShipTypeId(587);
            resp.setLogonDate(OffsetDateTime.parse("2026-11-01T10:00:00Z"));
            resp.setLogoffDate(OffsetDateTime.parse("2026-11-01T18:00:00Z"));

            when(corporationApi.queryCorporationMembersTrackingResponse(eq(CORPORATION_ID), eq(DATASOURCE), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(List.of(resp))));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            esiApiService.queryCorporationMemberTracking(CORPORATION_ID, ACCESS_TOKEN);

            // 第二次: userId=43
            userUtilMock.when(UserUtil::getUserId).thenReturn(43);

            String key43 = "esi:resp:" + DATASOURCE + ":43:/corporations/" + CORPORATION_ID + "/membertracking/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(43), eq("/corporations/" + CORPORATION_ID + "/membertracking/"), eq(0)))
                    .thenReturn(key43);
            when(esiResponseCache.getList(eq(key43), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(key43), any())).thenReturn(true);

            esiApiService.queryCorporationMemberTracking(CORPORATION_ID, ACCESS_TOKEN);

            // Assert: 两次 cacheKey 参数不同,且 key 各含 userId 段
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(esiResponseCache, atLeast(2)).put(keyCaptor.capture(), any(), any(Duration.class));

            List<String> capturedKeys = keyCaptor.getAllValues();
            assertThat(capturedKeys).contains(key42, key43);
            assertThat(key42).contains(":42:");
            assertThat(key43).contains(":43:");
            assertThat(key42).isNotEqualTo(key43);
        }
    }

    // ── AC: 403 转换 ──

    @Test
    @DisplayName("tracking_permissionLow_convertedToAccessException: SDK 403→EsiAccessException.permissionLow")
    void tracking_permissionLow_convertedToAccessException() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(42);

            String expectedKey = "esi:resp:" + DATASOURCE + ":42:/corporations/" + CORPORATION_ID + "/membertracking/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(42), eq("/corporations/" + CORPORATION_ID + "/membertracking/"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            // SDK 真实错误路径是异步 Mono.error(EsiStatusUtil.dataError()),非同步 throw;
            // callSdkMono 须经 onErrorMap 转换(IMP-1:异步 stub 钉住生产可达分支)
            when(corporationApi.queryCorporationMembersTrackingResponse(eq(CORPORATION_ID), eq(DATASOURCE), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.error(new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW)));

            // Act & Assert
            assertThatThrownBy(() -> esiApiService.queryCorporationMemberTracking(CORPORATION_ID, ACCESS_TOKEN))
                    .isInstanceOf(EsiAccessException.class)
                    .hasMessageContaining(EsiAccessException.MSG_PERMISSION_LOW);
        }
    }

    // ── MIN-G1: 空成员列表缓存命中（015 约定：空列表是合法缓存值） ──

    @Test
    @DisplayName("MIN-G1: 缓存返回空列表 → 命中缓存，不调 ESI 真调")
    void tracking_emptyListCacheHit_noRealCall() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(42);

            String expectedKey = "esi:resp:" + DATASOURCE + ":42:/corporations/" + CORPORATION_ID + "/membertracking/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(42), eq("/corporations/" + CORPORATION_ID + "/membertracking/"), eq(0)))
                    .thenReturn(expectedKey);
            // 缓存返回空列表（非 null）→ 应命中
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(List.of());

            // Act
            List<MemberTrackingItem> result = esiApiService.queryCorporationMemberTracking(CORPORATION_ID, ACCESS_TOKEN);

            // Assert: 返回空列表，ESI 真调从未发生
            assertThat(result).isEmpty();
            verify(corporationApi, never()).queryCorporationMembersTrackingResponse(anyInt(), anyString(), anyString());
            verify(esiResponseCache, never()).put(anyString(), any(), any(Duration.class));
        }
    }
}
