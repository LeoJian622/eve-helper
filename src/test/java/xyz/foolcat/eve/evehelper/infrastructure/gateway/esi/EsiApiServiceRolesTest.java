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
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterRolesInfo;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.esi.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiCacheTtlExtractor;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCache;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCacheProperties;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.ForceRefreshLimiter;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.RoleResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 角色军团权限网关查询单元测试(T029)。
 * <p>
 * 纯单元测试,不依赖 Spring 上下文;mock SDK API、缓存组件与限流器,
 * 验证 roles 映射、缓存命中、null 归一、权限判定 fixture 与缓存 TTL 写入。
 *
 * @author Leojan
 * date 2026-11-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色军团权限网关查询(T029)")
class EsiApiServiceRolesTest {

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

    private static final Integer CHARACTER_ID = 95465499;
    private static final Integer USER_ID = 100;
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

    // ── AC: 映射三集合 ──

    @Test
    @DisplayName("roles_mapsThreeCollections: SDK 返回三组角色→VO 三集合正确映射")
    void roles_mapsThreeCollections() {
        // Arrange: 缓存未命中,SDK 返回含三组角色的响应
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/roles/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/roles/"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            RoleResponse sdkBody = buildRoleResponse(
                    new String[]{"Director", "Accountant"},
                    new String[]{"Station_Manager"},
                    new String[]{"Diplomat"}
            );
            when(characterApi.queryCharacterRolesResponse(eq(CHARACTER_ID), eq(DATASOURCE), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            CharacterRolesInfo result = esiApiService.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN);

            // Assert: 三集合精确映射
            assertThat(result).isNotNull();
            assertThat(result.rolesAtHq()).containsExactly("Director", "Accountant");
            assertThat(result.rolesAtBase()).containsExactly("Station_Manager");
            assertThat(result.rolesAtOther()).containsExactly("Diplomat");
            verify(characterApi).queryCharacterRolesResponse(CHARACTER_ID, DATASOURCE, ACCESS_TOKEN);
        }
    }

    // ── AC: 缓存命中 ──

    @Test
    @DisplayName("roles_cacheHit_noSdkCall: 缓存命中→直接返回,不调用 SDK")
    void roles_cacheHit_noSdkCall() {
        // Arrange: 缓存命中
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/roles/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/roles/"), eq(0)))
                    .thenReturn(expectedKey);

            CharacterRolesInfo cachedInfo = new CharacterRolesInfo(
                    List.of("Director"), List.of("Station_Manager"), List.of());
            when(esiResponseCache.getList(eq(expectedKey), any()))
                    .thenReturn(List.of(cachedInfo));

            // Act
            CharacterRolesInfo result = esiApiService.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN);

            // Assert: 缓存命中,SDK 未被调用
            assertThat(result).isNotNull();
            assertThat(result.rolesAtHq()).containsExactly("Director");
            assertThat(result.rolesAtBase()).containsExactly("Station_Manager");
            verify(characterApi, never()).queryCharacterRolesResponse(anyInt(), anyString(), anyString());
        }
    }

    // ── AC: null 归一 ──

    @Test
    @DisplayName("roles_nullCollections_normalizedToEmptyList: SDK 返回 null 数组→归一为空列表")
    void roles_nullCollections_normalizedToEmptyList() {
        // Arrange: SDK 返回所有字段均为 null
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/roles/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/roles/"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            RoleResponse sdkBody = new RoleResponse();
            // 所有字段保持 null(默认值)
            when(characterApi.queryCharacterRolesResponse(eq(CHARACTER_ID), eq(DATASOURCE), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            CharacterRolesInfo result = esiApiService.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN);

            // Assert: null→空列表(非 null)
            assertThat(result).isNotNull();
            assertThat(result.rolesAtHq()).isNotNull().isEmpty();
            assertThat(result.rolesAtBase()).isNotNull().isEmpty();
            assertThat(result.rolesAtOther()).isNotNull().isEmpty();
        }
    }

    // ── fixture: 建筑权限 ──

    @Test
    @DisplayName("structurePermission_directorOrStationManager_inAnyCollection: Director 或 Station_Manager 出现于任一集合→true")
    void structurePermission_directorOrStationManager_inAnyCollection() {
        // Director 在 rolesAtHq
        assertThat(new CharacterRolesInfo(List.of("Director"), List.of(), List.of()).hasStructurePermission()).isTrue();
        // Director 在 rolesAtBase
        assertThat(new CharacterRolesInfo(List.of(), List.of("Director"), List.of()).hasStructurePermission()).isTrue();
        // Director 在 rolesAtOther
        assertThat(new CharacterRolesInfo(List.of(), List.of(), List.of("Director")).hasStructurePermission()).isTrue();
        // Station_Manager 在 rolesAtHq
        assertThat(new CharacterRolesInfo(List.of("Station_Manager"), List.of(), List.of()).hasStructurePermission()).isTrue();
        // Station_Manager 在 rolesAtBase
        assertThat(new CharacterRolesInfo(List.of(), List.of("Station_Manager"), List.of()).hasStructurePermission()).isTrue();
        // Station_Manager 在 rolesAtOther
        assertThat(new CharacterRolesInfo(List.of(), List.of(), List.of("Station_Manager")).hasStructurePermission()).isTrue();
        // 无权限反例: 仅有 Accountant
        assertThat(new CharacterRolesInfo(List.of("Accountant"), List.of(), List.of()).hasStructurePermission()).isFalse();
        // 无权限反例: 空集合
        assertThat(new CharacterRolesInfo(List.of(), List.of(), List.of()).hasStructurePermission()).isFalse();
    }

    // ── fixture: Director 追踪权限 ──

    @Test
    @DisplayName("directorRole_onlyDirector_notStationManager: 仅 Director 满足追踪权限,Station_Manager 单独→false")
    void directorRole_onlyDirector_notStationManager() {
        // Director 在任一集合→true
        assertThat(new CharacterRolesInfo(List.of("Director"), List.of(), List.of()).hasDirectorRole()).isTrue();
        assertThat(new CharacterRolesInfo(List.of(), List.of("Director"), List.of()).hasDirectorRole()).isTrue();
        assertThat(new CharacterRolesInfo(List.of(), List.of(), List.of("Director")).hasDirectorRole()).isTrue();
        // Station_Manager 单独→false(追踪不接受 Station_Manager)
        assertThat(new CharacterRolesInfo(List.of("Station_Manager"), List.of(), List.of()).hasDirectorRole()).isFalse();
        assertThat(new CharacterRolesInfo(List.of(), List.of("Station_Manager"), List.of()).hasDirectorRole()).isFalse();
        // Accountant→false
        assertThat(new CharacterRolesInfo(List.of("Accountant"), List.of(), List.of()).hasDirectorRole()).isFalse();
        // 空集合→false
        assertThat(new CharacterRolesInfo(List.of(), List.of(), List.of()).hasDirectorRole()).isFalse();
    }

    // ── fixture: 工业权限 ──

    @Test
    @DisplayName("industryPermission_directorOrAccountant_inAnyCollection: Director 或 Accountant 出现于任一集合→true")
    void industryPermission_directorOrAccountant_inAnyCollection() {
        // Director 在三角色位置
        assertThat(new CharacterRolesInfo(List.of("Director"), List.of(), List.of()).hasIndustryPermission()).isTrue();
        assertThat(new CharacterRolesInfo(List.of(), List.of("Director"), List.of()).hasIndustryPermission()).isTrue();
        assertThat(new CharacterRolesInfo(List.of(), List.of(), List.of("Director")).hasIndustryPermission()).isTrue();
        // Accountant 在三角色位置
        assertThat(new CharacterRolesInfo(List.of("Accountant"), List.of(), List.of()).hasIndustryPermission()).isTrue();
        assertThat(new CharacterRolesInfo(List.of(), List.of("Accountant"), List.of()).hasIndustryPermission()).isTrue();
        assertThat(new CharacterRolesInfo(List.of(), List.of(), List.of("Accountant")).hasIndustryPermission()).isTrue();
        // 无权限反例: 仅有 Station_Manager
        assertThat(new CharacterRolesInfo(List.of("Station_Manager"), List.of(), List.of()).hasIndustryPermission()).isFalse();
        // 无权限反例: 仅有 Diplomat
        assertThat(new CharacterRolesInfo(List.of(), List.of("Diplomat"), List.of()).hasIndustryPermission()).isFalse();
        // 空集合→false
        assertThat(new CharacterRolesInfo(List.of(), List.of(), List.of()).hasIndustryPermission()).isFalse();
    }

    // ── TTL 写入 ──

    @Test
    @DisplayName("roles_cacheWrite_usesHeaderTtl: 缓存写使用响应头提取的 TTL(非 null)")
    void roles_cacheWrite_usesHeaderTtl() {
        // Arrange
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            String expectedKey = "esi:resp:" + DATASOURCE + ":" + USER_ID + ":/characters/" + CHARACTER_ID + "/roles/:0";
            when(esiResponseCache.cacheKey(eq(DATASOURCE), eq(USER_ID), eq("/characters/" + CHARACTER_ID + "/roles/"), eq(0)))
                    .thenReturn(expectedKey);
            when(esiResponseCache.getList(eq(expectedKey), any())).thenReturn(null);
            when(esiResponseCache.tryClaim(eq(expectedKey), any())).thenReturn(true);

            // extractor 被 mock,头设置仅为可读性
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=300");
            RoleResponse sdkBody = buildRoleResponse(
                    new String[]{"Director"},
                    new String[]{"Station_Manager"},
                    new String[]{"Accountant"}
            );
            when(characterApi.queryCharacterRolesResponse(eq(CHARACTER_ID), eq(DATASOURCE), eq(ACCESS_TOKEN)))
                    .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(sdkBody)));
            when(esiCacheTtlExtractor.extract(any(HttpHeaders.class))).thenReturn(EXPECTED_TTL);

            // Act
            esiApiService.queryCharacterRoles(CHARACTER_ID, ACCESS_TOKEN);

            // Assert: put 收到非 null TTL 且等于从响应头提取的值
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(esiResponseCache).put(eq(expectedKey), any(), ttlCaptor.capture());
            assertThat(ttlCaptor.getValue()).isNotNull();
            assertThat(ttlCaptor.getValue()).isEqualTo(EXPECTED_TTL);
        }
    }

    // ── helpers ──

    private RoleResponse buildRoleResponse(String[] rolesAtHq, String[] rolesAtBase, String[] rolesAtOther) {
        RoleResponse resp = new RoleResponse();
        resp.setRolesAtHq(rolesAtHq);
        resp.setRolesAtBase(rolesAtBase);
        resp.setRolesAtOther(rolesAtOther);
        return resp;
    }
}
