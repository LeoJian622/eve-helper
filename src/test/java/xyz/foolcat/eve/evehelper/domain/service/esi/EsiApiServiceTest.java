package xyz.foolcat.eve.evehelper.domain.service.esi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.AssetsApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CharacterApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CorporationApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.IndustryApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.PageTotalApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.WalletApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCache;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCacheProperties;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.ForceRefreshLimiter;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AssetResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.IndustryJobPlacedResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.Id2NameResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ObserverMiningLedgerResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.TypeInfoResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletJournalResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletTransactionsResponse;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EsiApiService.getAuthorizationStatus 单元测试(纯 Mockito,不依赖 Spring/DB/Redis)。
 * <p>
 * 覆盖四态判定(AUTHORIZED/EXPIRED/NOT_AUTHORIZED/UNKNOWN)+ 缓存命中/未命中路径,
 * 对应 003-esi-auth-status 任务 T003-T006 + T011。
 * <p>
 * EsiApiService 已迁至 infrastructure/external/esi(004 US1),测试 import 用 infra 路径,
 * 测试文件位置按 tasks.md 保持放在 domain/service/esi 下。
 *
 * @author Leojan
 * date 2026-08-07
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ESI 授权状态判定单元测试 - 四态 + 缓存")
class EsiApiServiceTest {

    @MockBean
    CacheGateway cacheGateway;

    @MockBean
    EveAccountService eveAccountService;

    @MockBean
    AuthorizeOAuth authorizeOAuth;

    @MockBean
    CharacterApi characterApi;

    @MockBean
    UniverseApi universeApi;

    @MockBean
    AssetsApi assetsApi;

    @MockBean
    IndustryApi industryApi;

    @MockBean
    CorporationApi corporationApi;

    @MockBean
    WalletApi walletApi;

    @MockBean
    ForceRefreshLimiter forceRefreshLimiter;

    @Autowired
    private EsiApiService esiApiService;

    @Autowired
    private EsiResponseCache esiResponseCache;

    @Autowired
    private EsiResponseCacheProperties esiResponseCacheProperties;

    private static final Integer USER_ID = 100;
    private static final Integer CID = 95465499;
    private static final Integer CORP_ID = 999888;
    private static final Long OBSERVER_ID = 888777L;
    private static final String LANG = "es";
    private static final int DIVISION = 2;
    private static final Long FROM_ID = 555444L;
    private static final Integer TYPE_ID = 34;
    private static final String STATUS_KEY = "esi_auth_status:" + CID;
    private static final String LOCK_KEY = "esi_refresh_lock:" + CID;
    private static final String ACCESS_TOKEN_KEY = "esi_access_token:" + USER_ID + ":" + CID;

    /**
     * 构造一个含 userId/characterId/refreshToken 的测试角色。
     */
    private EveAccount account(String refreshToken) {
        EveAccount a = new EveAccount();
        a.setUserId(USER_ID);
        a.setCharacterId(CID);
        a.setRefreshToken(refreshToken);
        return a;
    }

    /**
     * 模拟缓存未命中 + 成功获取 per-character 锁。
     * cacheGateway.get 被调用两次(锁前检查 + 持锁后 double-check),均返回 null。
     */
    private void stubCacheMissAndLockAcquired() {
        when(cacheGateway.get(STATUS_KEY)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
    }

    // ── T003: refreshToken 为空 -> NOT_AUTHORIZED ──

    @Test
    @DisplayName("refreshToken 为空或 null -> NOT_AUTHORIZED,不触发 ESI/缓存/锁")
    void blankRefreshToken_returnsNotAuthorized() {
        assertEquals(EsiAuthStatus.NOT_AUTHORIZED, esiApiService.getAuthorizationStatus(account("")));
        assertEquals(EsiAuthStatus.NOT_AUTHORIZED, esiApiService.getAuthorizationStatus(account(null)));

        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
        verify(cacheGateway, never()).setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    // ── T006: 缓存命中 -> 直接返回,不调 ESI ──

    @Test
    @DisplayName("状态缓存命中(AUTHORIZED) -> 直接返回,不调用 authorizeOAuth")
    void cacheHit_returnsCachedStatus() {
        EveAccount acc = account("rt");
        when(cacheGateway.get(STATUS_KEY)).thenReturn("AUTHORIZED");

        EsiAuthStatus status = esiApiService.getAuthorizationStatus(acc);

        assertEquals(EsiAuthStatus.AUTHORIZED, status);
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
        verify(cacheGateway, never()).setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    // ── T004: 缓存未命中 + 刷新成功 -> AUTHORIZED ──

    @Test
    @DisplayName("缓存未命中 + 刷新成功 -> AUTHORIZED,用 ArgumentCaptor 验证缓存写入键/值")
    void cacheMiss_refreshSuccess_returnsAuthorized() {
        EveAccount acc = account("old-rt");
        AuthTokenResponse token = new AuthTokenResponse();
        token.setAccessToken("at");
        token.setRefreshToken("new-rt");
        stubCacheMissAndLockAcquired();
        when(eveAccountService.insertOrUpdateSelective(any(EveAccount.class))).thenReturn(1);
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "old-rt")).thenReturn(Mono.just(token));

        EsiAuthStatus status = esiApiService.getAuthorizationStatus(acc);

        assertEquals(EsiAuthStatus.AUTHORIZED, status);

        // 验证 refreshToken 回写(新 token 与旧 token 不同时触发 insertOrUpdateSelective)
        ArgumentCaptor<EveAccount> accountCaptor = ArgumentCaptor.forClass(EveAccount.class);
        verify(eveAccountService).insertOrUpdateSelective(accountCaptor.capture());
        assertEquals(CID, accountCaptor.getValue().getCharacterId());
        assertEquals("new-rt", accountCaptor.getValue().getRefreshToken());

        // 用 ArgumentCaptor 验证缓存写入键/值(set 被调用两次:accessToken + status)
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway, times(2)).set(keyCaptor.capture(), valueCaptor.capture(), anyLong(), eq(TimeUnit.SECONDS));
        // 第一次:accessToken 缓存
        assertEquals(ACCESS_TOKEN_KEY, keyCaptor.getAllValues().get(0));
        assertEquals("Bearer at", valueCaptor.getAllValues().get(0));
        // 第二次:状态缓存
        assertEquals(STATUS_KEY, keyCaptor.getAllValues().get(1));
        assertEquals("AUTHORIZED", valueCaptor.getAllValues().get(1));

        // 锁释放在 finally 中执行
        verify(cacheGateway).delete(LOCK_KEY);
    }

    // ── T005: ESI_AUTHORIZATION_FAILURE(4xx) -> EXPIRED ──

    @Test
    @DisplayName("缓存未命中 + EsiException(ESI_AUTHORIZATION_FAILURE) -> EXPIRED")
    void cacheMiss_authFailure_returnsExpired() {
        EveAccount acc = account("rt");
        stubCacheMissAndLockAcquired();
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt"))
                .thenReturn(Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE)));

        EsiAuthStatus status = esiApiService.getAuthorizationStatus(acc);

        assertEquals(EsiAuthStatus.EXPIRED, status);
        // 授权失败时不回写 token
        verify(eveAccountService, never()).insertOrUpdateSelective(any(EveAccount.class));
        verify(cacheGateway).set(eq(STATUS_KEY), eq("EXPIRED"), anyLong(), eq(TimeUnit.SECONDS));
        verify(cacheGateway).delete(LOCK_KEY);
    }

    // ── T011a: ESI_SERVER_FAILURE(5xx) -> UNKNOWN ──

    @Test
    @DisplayName("缓存未命中 + EsiException(ESI_SERVER_FAILURE) -> UNKNOWN")
    void cacheMiss_serverFailure_returnsUnknown() {
        EveAccount acc = account("rt");
        stubCacheMissAndLockAcquired();
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt"))
                .thenReturn(Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE)));

        EsiAuthStatus status = esiApiService.getAuthorizationStatus(acc);

        assertEquals(EsiAuthStatus.UNKNOWN, status);
        verify(cacheGateway).set(eq(STATUS_KEY), eq("UNKNOWN"), anyLong(), eq(TimeUnit.SECONDS));
        verify(cacheGateway).delete(LOCK_KEY);
    }

    // ── T011b: TimeoutException -> UNKNOWN ──

    @Test
    @DisplayName("缓存未命中 + TimeoutException -> UNKNOWN")
    void cacheMiss_timeout_returnsUnknown() {
        EveAccount acc = account("rt");
        stubCacheMissAndLockAcquired();
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt"))
                .thenReturn(Mono.error(new TimeoutException()));

        EsiAuthStatus status = esiApiService.getAuthorizationStatus(acc);

        assertEquals(EsiAuthStatus.UNKNOWN, status);
        verify(cacheGateway).set(eq(STATUS_KEY), eq("UNKNOWN"), anyLong(), eq(TimeUnit.SECONDS));
        verify(cacheGateway).delete(LOCK_KEY);
    }

    // ─═ 015/T008:角色资产查询接入缓存判读(列表 + maxPage) ═─

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 以指定 userId 认证(主体为 Number,UserUtil 走 instanceof Number 分支)。
     */
    private void authAs(Integer userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null));
    }

    /**
     * 匿名上下文(主体为 String,UserUtil 返回 -1)。
     */
    private void authAsAnonymous() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("anonymousUser", null));
    }

    /**
     * 构造与 {@link EsiApiService#queryCharactersAssets} 内部一致的列表缓存键(含 datasource/userId/uri/page)。
     */
    private String assetsKey(int page) {
        return esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/characters/" + CID + "/assets/", page);
    }

    /**
     * 构造与 {@link EsiApiService#queryCharactersAssetsMaxPage} 内部一致的 maxPage 独立缓存键。
     */
    private String assetsMaxPageKey() {
        return esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/characters/" + CID + "/assets/", "maxPage");
    }

    private static AssetResponse assetResponse(long itemId) {
        AssetResponse ar = new AssetResponse();
        ar.setItemId(itemId);
        ar.setTypeId(34);
        return ar;
    }

    private static HttpHeaders maxAge300() {
        HttpHeaders h = new HttpHeaders();
        h.setCacheControl("public, max-age=300");
        return h;
    }

    // ── 列表:缓存命中/未命中/无头/未认证 ──

    @Test
    @DisplayName("资产列表:缓存命中 → 返回缓存数据,不触发 AssetsApi 真调(SC-001)")
    void characterAssets_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn("[{\"itemId\":100,\"typeId\":34}]");

        List<Assets> result = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(100L);
        verify(assetsApi, never()).queryCharactersAssetsResponse(any(), anyString(), any(), anyString());
        verify(assetsApi, never()).queryCharactersAssets(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("资产列表:未命中 → 真调一次并按 max-age 回填")
    void characterAssets_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn(null);
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(200L)))));

        List<Assets> result = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(200L);
        // 真调一次
        verify(assetsApi).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
        // 回填:set(key, JSON序列化列表, 3600s, SECONDS)
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("资产列表:无响应头 → 每次真调,不回填缓存")
    void characterAssets_noHeader_realCallEveryTime() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn(null);
        // 无 Cache-Control 头
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok().body(List.of(assetResponse(300L)))));

        esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();
        esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();

        verify(assetsApi, times(2)).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
        verify(cacheGateway, never()).set(anyString(), any(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("资产列表:未认证(userId=-1) → 直接真调原 Flux 入口,不进缓存")
    void characterAssets_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(assetsApi.queryCharactersAssets(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Flux.just(assetResponse(400L)));

        List<Assets> result = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(400L);
        verify(assetsApi).queryCharactersAssets(eq(CID), eq("serenity"), eq(1), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    // ── maxPage:缓存命中/未命中/无头/未认证 ──

    @Test
    @DisplayName("资产 maxPage:缓存命中 → 直接返回,不真调")
    void characterAssetsMaxPage_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = assetsMaxPageKey();
        when(cacheGateway.get(key)).thenReturn(20);

        Integer mp = esiApiService.queryCharactersAssetsMaxPage(CID, "Bearer x");

        assertThat(mp).isEqualTo(20);
        verify(assetsApi, never()).queryCharactersAssetsMaxPage(any(), anyString(), anyString());
        verify(assetsApi, never()).queryCharactersAssetsMaxPageResponse(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("资产 maxPage:未命中 → 真调含头入口并按 max-age 回填 Integer(:maxPage 独立键)")
    void characterAssetsMaxPage_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = assetsMaxPageKey();
        // maxPage 独立键必须与列表首页 page=1 键不同(T008 评审 M1 修复回归)
        assertThat(key).endsWith(":maxPage");
        assertThat(key).isNotEqualTo(assetsKey(1));
        when(cacheGateway.get(key)).thenReturn(null);
        when(assetsApi.queryCharactersAssetsMaxPageResponse(eq(CID), eq("serenity"), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(5, maxAge300()));

        Integer mp = esiApiService.queryCharactersAssetsMaxPage(CID, "Bearer x");

        assertThat(mp).isEqualTo(5);
        verify(assetsApi).queryCharactersAssetsMaxPageResponse(eq(CID), eq("serenity"), anyString());
        verify(cacheGateway).set(eq(key), eq(5), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("资产 maxPage:无响应头 → 不回填,真调结果照常返回")
    void characterAssetsMaxPage_noHeader_noCacheWrite() {
        authAs(USER_ID);
        String key = assetsMaxPageKey();
        when(cacheGateway.get(key)).thenReturn(null);
        when(assetsApi.queryCharactersAssetsMaxPageResponse(eq(CID), eq("serenity"), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(3, new HttpHeaders()));

        Integer mp = esiApiService.queryCharactersAssetsMaxPage(CID, "Bearer x");

        assertThat(mp).isEqualTo(3);
        verify(cacheGateway, never()).set(anyString(), any(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("资产 maxPage:未认证 → 直接真调原方法,不进缓存")
    void characterAssetsMaxPage_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(assetsApi.queryCharactersAssetsMaxPage(eq(CID), eq("serenity"), anyString())).thenReturn(7);

        Integer mp = esiApiService.queryCharactersAssetsMaxPage(CID, "Bearer x");

        assertThat(mp).isEqualTo(7);
        verify(assetsApi).queryCharactersAssetsMaxPage(eq(CID), eq("serenity"), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    // ─═ 015/T009:工业/蓝图类查询接入缓存判读(列表 + maxPage) ═─

    private String cacheKeyFor(String uri, Object pageOrToken) {
        if (pageOrToken instanceof Integer page) {
            return esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID, uri, page);
        }
        return esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID, uri, String.valueOf(pageOrToken));
    }

    private static IndustryJobPlacedResponse industryJob(int jobId) {
        IndustryJobPlacedResponse r = new IndustryJobPlacedResponse();
        r.setJobId(jobId);
        r.setActivityId(2);
        return r;
    }

    // ── 人物工业任务列表 ──

    @Test
    @DisplayName("人物工业任务:缓存命中 → 返回缓存数据,不真调")
    void characterIndustryJobs_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/characters/" + CID + "/industry/jobs/", 0);
        when(cacheGateway.get(key)).thenReturn("[{\"jobId\":10,\"activityId\":2}]");

        List<IndustryJob> result = esiApiService.queryCharacterIndustryJobs(CID, true, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo(10);
        verify(industryApi, never()).queryCharacterIndustryJobsResponse(any(), anyString(), any(), anyString());
        verify(industryApi, never()).queryCharacterIndustryJobs(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("人物工业任务:未命中 → 真调一次并按 max-age 回填")
    void characterIndustryJobs_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/characters/" + CID + "/industry/jobs/", 0);
        when(cacheGateway.get(key)).thenReturn(null);
        when(industryApi.queryCharacterIndustryJobsResponse(eq(CID), eq("serenity"), eq(true), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(industryJob(20)))));

        List<IndustryJob> result = esiApiService.queryCharacterIndustryJobs(CID, true, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo(20);
        verify(industryApi).queryCharacterIndustryJobsResponse(eq(CID), eq("serenity"), eq(true), anyString());
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("人物工业任务:未认证 → 直接真调原 Flux 入口,不进缓存")
    void characterIndustryJobs_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(industryApi.queryCharacterIndustryJobs(eq(CID), eq("serenity"), eq(true), anyString()))
                .thenReturn(Flux.just(industryJob(30)));

        List<IndustryJob> result = esiApiService.queryCharacterIndustryJobs(CID, true, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo(30);
        verify(industryApi).queryCharacterIndustryJobs(eq(CID), eq("serenity"), eq(true), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    // ── 军团工业任务列表 ──

    @Test
    @DisplayName("军团工业任务:缓存命中 → 返回缓存数据,不真调")
    void corporationIndustryJobs_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/industry/jobs/", 0);
        when(cacheGateway.get(key)).thenReturn("[{\"jobId\":40,\"activityId\":2}]");

        List<IndustryJob> result = esiApiService.queryCorporationIndustryJobs(CORP_ID, false, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo(40);
        verify(industryApi, never()).queryCorporationIndustryJobsResponse(any(), anyString(), any(), anyString());
        verify(industryApi, never()).queryCorporationIndustryJobs(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("军团工业任务:未命中 → 真调一次并按 max-age 回填")
    void corporationIndustryJobs_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/industry/jobs/", 0);
        when(cacheGateway.get(key)).thenReturn(null);
        when(industryApi.queryCorporationIndustryJobsResponse(eq(CORP_ID), eq("serenity"), eq(false), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(industryJob(50)))));

        List<IndustryJob> result = esiApiService.queryCorporationIndustryJobs(CORP_ID, false, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo(50);
        verify(industryApi).queryCorporationIndustryJobsResponse(eq(CORP_ID), eq("serenity"), eq(false), anyString());
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("军团工业任务:未认证 → 直接真调原 Flux 入口,不进缓存")
    void corporationIndustryJobs_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(industryApi.queryCorporationIndustryJobs(eq(CORP_ID), eq("serenity"), eq(false), anyString()))
                .thenReturn(Flux.just(industryJob(60)));

        List<IndustryJob> result = esiApiService.queryCorporationIndustryJobs(CORP_ID, false, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo(60);
        verify(industryApi).queryCorporationIndustryJobs(eq(CORP_ID), eq("serenity"), eq(false), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    // ── 军团蓝图 maxPage(独立 :maxPage 键) ──

    @Test
    @DisplayName("军团蓝图 maxPage:缓存命中 → 直接返回,不真调")
    void corporationBlueprintsMaxPage_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/blueprints/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(12);

        Integer mp = esiApiService.queryCorporationBlueprintsMaxPage(CORP_ID, "Bearer x");

        assertThat(mp).isEqualTo(12);
        verify(corporationApi, never()).queryCorporationBlueprintsMaxPage(any(), anyString(), anyString());
        verify(corporationApi, never()).queryCorporationBlueprintsMaxPageResponse(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("军团蓝图 maxPage:未命中 → 真调含头入口并按 max-age 回填 Integer(:maxPage 键)")
    void corporationBlueprintsMaxPage_cacheMiss_usesMaxPageKeyAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/blueprints/", "maxPage");
        // 列表首页键(page=1)与 maxPage 键必须不同(T008 评审 M1)
        assertThat(key).endsWith(":maxPage");
        assertThat(key).isNotEqualTo(esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/corporations/" + CORP_ID + "/blueprints/", 1));
        when(cacheGateway.get(key)).thenReturn(null);
        when(corporationApi.queryCorporationBlueprintsMaxPageResponse(eq(CORP_ID), eq("serenity"), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(12, maxAge300()));

        Integer mp = esiApiService.queryCorporationBlueprintsMaxPage(CORP_ID, "Bearer x");

        assertThat(mp).isEqualTo(12);
        verify(corporationApi).queryCorporationBlueprintsMaxPageResponse(eq(CORP_ID), eq("serenity"), anyString());
        verify(cacheGateway).set(eq(key), eq(12), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("军团蓝图 maxPage:无响应头 → 不回填,真调结果照常返回")
    void corporationBlueprintsMaxPage_noHeader_noCacheWrite() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/blueprints/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(null);
        when(corporationApi.queryCorporationBlueprintsMaxPageResponse(eq(CORP_ID), eq("serenity"), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(3, new HttpHeaders()));

        Integer mp = esiApiService.queryCorporationBlueprintsMaxPage(CORP_ID, "Bearer x");

        assertThat(mp).isEqualTo(3);
        verify(cacheGateway, never()).set(anyString(), any(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("军团蓝图 maxPage:未认证 → 直接真调原方法,不进缓存")
    void corporationBlueprintsMaxPage_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(corporationApi.queryCorporationBlueprintsMaxPage(eq(CORP_ID), eq("serenity"), anyString())).thenReturn(9);

        Integer mp = esiApiService.queryCorporationBlueprintsMaxPage(CORP_ID, "Bearer x");

        assertThat(mp).isEqualTo(9);
        verify(corporationApi).queryCorporationBlueprintsMaxPage(eq(CORP_ID), eq("serenity"), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    // ── 军团工业任务 maxPage(独立 :maxPage 键) ──

    @Test
    @DisplayName("军团工业任务 maxPage:缓存命中 → 直接返回,不真调")
    void corporationIndustryJobsMaxPage_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/industry/jobs/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(4);

        Integer mp = esiApiService.queryCorporationIndustryJobsMaxPage(CORP_ID, true, "Bearer x");

        assertThat(mp).isEqualTo(4);
        verify(industryApi, never()).queryCorporationIndustryJobsMaxPage(any(), anyString(), any(), anyString());
        verify(industryApi, never()).queryCorporationIndustryJobsMaxPageResponse(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("军团工业任务 maxPage:未命中 → 真调含头入口并按 max-age 回填 Integer(:maxPage 键)")
    void corporationIndustryJobsMaxPage_cacheMiss_usesMaxPageKeyAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/industry/jobs/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(null);
        when(industryApi.queryCorporationIndustryJobsMaxPageResponse(eq(CORP_ID), eq("serenity"), eq(true), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(4, maxAge300()));

        Integer mp = esiApiService.queryCorporationIndustryJobsMaxPage(CORP_ID, true, "Bearer x");

        assertThat(mp).isEqualTo(4);
        verify(industryApi).queryCorporationIndustryJobsMaxPageResponse(eq(CORP_ID), eq("serenity"), eq(true), anyString());
        verify(cacheGateway).set(eq(key), eq(4), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("军团工业任务 maxPage:未认证 → 直接真调原方法,不进缓存")
    void corporationIndustryJobsMaxPage_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(industryApi.queryCorporationIndustryJobsMaxPage(eq(CORP_ID), eq("serenity"), eq(true), anyString()))
                .thenReturn(6);

        Integer mp = esiApiService.queryCorporationIndustryJobsMaxPage(CORP_ID, true, "Bearer x");

        assertThat(mp).isEqualTo(6);
        verify(industryApi).queryCorporationIndustryJobsMaxPage(eq(CORP_ID), eq("serenity"), eq(true), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    // ─═ 015/T010:采矿/建筑类查询接入缓存判读(列表 + maxPage) ═─

    private static ObserverMiningLedgerResponse miningLedger(int typeId) {
        ObserverMiningLedgerResponse r = new ObserverMiningLedgerResponse();
        r.setTypeId(typeId);
        r.setQuantity(3L);
        return r;
    }

    private static StructuresInformationResponse structureResp(long id) {
        StructuresInformationResponse r = new StructuresInformationResponse();
        r.setStructureId(id);
        r.setName("Fort");
        return r;
    }

    // ── 军团单建筑采矿列表 ──

    @Test
    @DisplayName("采矿列表:缓存命中 → 返回缓存数据,不真调")
    void miningObserver_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/mining/observers/" + OBSERVER_ID + "/", 1);
        when(cacheGateway.get(key)).thenReturn("[{\"typeId\":34,\"quantity\":3}]");

        List<MiningDetail> result = esiApiService.queryCorporationMiningObserver(CORP_ID, OBSERVER_ID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeId()).isEqualTo(34);
        verify(industryApi, never()).queryCorporationMiningObserverResponse(any(), anyString(), any(), any(), anyString());
        verify(industryApi, never()).queryCorporationMiningObserver(any(), anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("采矿列表:未命中 → 真调一次并按 max-age 回填")
    void miningObserver_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/mining/observers/" + OBSERVER_ID + "/", 1);
        when(cacheGateway.get(key)).thenReturn(null);
        when(industryApi.queryCorporationMiningObserverResponse(eq(CORP_ID), eq("serenity"), eq(OBSERVER_ID), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(miningLedger(34)))));

        List<MiningDetail> result = esiApiService.queryCorporationMiningObserver(CORP_ID, OBSERVER_ID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeId()).isEqualTo(34);
        verify(industryApi).queryCorporationMiningObserverResponse(eq(CORP_ID), eq("serenity"), eq(OBSERVER_ID), eq(1), anyString());
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("采矿列表:未认证 → 直接真调原 Flux 入口,不进缓存")
    void miningObserver_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(industryApi.queryCorporationMiningObserver(eq(CORP_ID), eq("serenity"), eq(OBSERVER_ID), eq(1), anyString()))
                .thenReturn(Flux.just(miningLedger(34)));

        List<MiningDetail> result = esiApiService.queryCorporationMiningObserver(CORP_ID, OBSERVER_ID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(industryApi).queryCorporationMiningObserver(eq(CORP_ID), eq("serenity"), eq(OBSERVER_ID), eq(1), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    @Test
    @DisplayName("采矿列表:M1 隔离 — observerId 不同 → 缓存键不同")
    void miningObserverKey_differsByObserverId() {
        String k1 = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/corporations/" + CORP_ID + "/mining/observers/" + OBSERVER_ID + "/", 1);
        String k2 = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/corporations/" + CORP_ID + "/mining/observers/" + (OBSERVER_ID + 1) + "/", 1);
        assertThat(k1).isNotEqualTo(k2);
    }

    // ── 军团单建筑采矿 maxPage(独立 :maxPage 键) ──

    @Test
    @DisplayName("采矿 maxPage:缓存命中 → 直接返回,不真调")
    void miningObserverMaxPage_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/mining/observers/" + OBSERVER_ID + "/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(5);

        Integer mp = esiApiService.queryCorporationMiningObserverMaxPage(CORP_ID, OBSERVER_ID, "Bearer x");

        assertThat(mp).isEqualTo(5);
        verify(industryApi, never()).queryCorporationMiningObserverMaxPage(any(), any(), anyString(), anyString());
        verify(industryApi, never()).queryCorporationMiningObserverMaxPageResponse(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("采矿 maxPage:未命中 → 真调含头入口并按 max-age 回填 Integer")
    void miningObserverMaxPage_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/mining/observers/" + OBSERVER_ID + "/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(null);
        when(industryApi.queryCorporationMiningObserverMaxPageResponse(eq(CORP_ID), eq("serenity"), eq(OBSERVER_ID), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(5, maxAge300()));

        Integer mp = esiApiService.queryCorporationMiningObserverMaxPage(CORP_ID, OBSERVER_ID, "Bearer x");

        assertThat(mp).isEqualTo(5);
        verify(industryApi).queryCorporationMiningObserverMaxPageResponse(eq(CORP_ID), eq("serenity"), eq(OBSERVER_ID), anyString());
        verify(cacheGateway).set(eq(key), eq(5), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("采矿 maxPage:未认证 → 直接真调原方法,不进缓存")
    void miningObserverMaxPage_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(industryApi.queryCorporationMiningObserverMaxPage(eq(CORP_ID), eq(OBSERVER_ID), eq("serenity"), anyString()))
                .thenReturn(5);

        Integer mp = esiApiService.queryCorporationMiningObserverMaxPage(CORP_ID, OBSERVER_ID, "Bearer x");

        assertThat(mp).isEqualTo(5);
        verify(industryApi).queryCorporationMiningObserverMaxPage(eq(CORP_ID), eq(OBSERVER_ID), eq("serenity"), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    // ── 军团建筑列表 ──

    @Test
    @DisplayName("建筑列表:缓存命中 → 返回缓存数据,不真调")
    void structures_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/structures/" + LANG + "/", 1);
        when(cacheGateway.get(key)).thenReturn("[{\"structureId\":10,\"name\":\"Fort\"}]");

        List<Structure> result = esiApiService.queryCorporationStructures(CORP_ID, LANG, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStructureId()).isEqualTo(10L);
        verify(corporationApi, never()).queryCorporationStructuresResponse(any(), anyString(), anyString(), any(), anyString());
        verify(corporationApi, never()).queryCorporationStructures(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("建筑列表:未命中 → 真调一次并按 max-age 回填")
    void structures_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/structures/" + LANG + "/", 1);
        when(cacheGateway.get(key)).thenReturn(null);
        when(corporationApi.queryCorporationStructuresResponse(eq(CORP_ID), eq("serenity"), eq(LANG), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(structureResp(10L)))));

        List<Structure> result = esiApiService.queryCorporationStructures(CORP_ID, LANG, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStructureId()).isEqualTo(10L);
        verify(corporationApi).queryCorporationStructuresResponse(eq(CORP_ID), eq("serenity"), eq(LANG), eq(1), anyString());
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("建筑列表:未认证 → 直接真调原 Flux 入口,不进缓存")
    void structures_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(corporationApi.queryCorporationStructures(eq(CORP_ID), eq("serenity"), eq(LANG), eq(1), anyString()))
                .thenReturn(Flux.just(structureResp(10L)));

        List<Structure> result = esiApiService.queryCorporationStructures(CORP_ID, LANG, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(corporationApi).queryCorporationStructures(eq(CORP_ID), eq("serenity"), eq(LANG), eq(1), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    @Test
    @DisplayName("建筑列表:M1 隔离 — language 不同 → 缓存键不同")
    void structuresKey_differsByLanguage() {
        String en = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/corporations/" + CORP_ID + "/structures/en/", 1);
        String es = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/corporations/" + CORP_ID + "/structures/es/", 1);
        assertThat(en).isNotEqualTo(es);
    }

    // ── 军团建筑 maxPage(独立 :maxPage 键,语言无关) ──

    @Test
    @DisplayName("建筑 maxPage:缓存命中 → 直接返回,不真调")
    void structuresMaxPage_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/structures/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(7);

        Integer mp = esiApiService.queryCorporationStructuresMaxPage(CORP_ID, "Bearer x");

        assertThat(mp).isEqualTo(7);
        verify(corporationApi, never()).queryCorporationStructuresMaxPage(any(), anyString(), anyString());
        verify(corporationApi, never()).queryCorporationStructuresMaxPageResponse(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("建筑 maxPage:未命中 → 真调含头入口并按 max-age 回填 Integer")
    void structuresMaxPage_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/structures/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(null);
        when(corporationApi.queryCorporationStructuresMaxPageResponse(eq(CORP_ID), eq("serenity"), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(7, maxAge300()));

        Integer mp = esiApiService.queryCorporationStructuresMaxPage(CORP_ID, "Bearer x");

        assertThat(mp).isEqualTo(7);
        verify(corporationApi).queryCorporationStructuresMaxPageResponse(eq(CORP_ID), eq("serenity"), anyString());
        verify(cacheGateway).set(eq(key), eq(7), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("建筑 maxPage:未认证 → 直接真调原方法,不进缓存")
    void structuresMaxPage_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(corporationApi.queryCorporationStructuresMaxPage(eq(CORP_ID), eq("serenity"), anyString())).thenReturn(11);

        Integer mp = esiApiService.queryCorporationStructuresMaxPage(CORP_ID, "Bearer x");

        assertThat(mp).isEqualTo(11);
        verify(corporationApi).queryCorporationStructuresMaxPage(eq(CORP_ID), eq("serenity"), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    // ─═ 015/T011:钱包流水/交易类查询接入缓存判读 ═─

    private static WalletJournalResponse journalResp() {
        WalletJournalResponse r = new WalletJournalResponse();
        r.setRefType("wallet");
        r.setDescription("x");
        return r;
    }

    private static WalletTransactionsResponse txnResp() {
        return new WalletTransactionsResponse();
    }

    @Test
    @DisplayName("人物钱包流水:缓存命中 → 返回缓存数据,不真调")
    void charJournal_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/characters/" + CID + "/wallet/journal/", 1);
        when(cacheGateway.get(key)).thenReturn("[{\"id\":1}]");

        List<WalletJournal> result = esiApiService.queryCharacterWalletJournal(CID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(walletApi, never()).queryCharacterWalletJournalResponse(any(), anyString(), any(), anyString());
        verify(walletApi, never()).queryCharacterWalletJournal(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("人物钱包流水:未命中 → 真调一次并按 max-age 回填")
    void charJournal_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/characters/" + CID + "/wallet/journal/", 1);
        when(cacheGateway.get(key)).thenReturn(null);
        when(walletApi.queryCharacterWalletJournalResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(journalResp()))));

        List<WalletJournal> result = esiApiService.queryCharacterWalletJournal(CID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(walletApi).queryCharacterWalletJournalResponse(eq(CID), eq("serenity"), eq(1), anyString());
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("人物钱包流水:未认证 → 直接真调原 Flux 入口,不进缓存")
    void charJournal_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(walletApi.queryCharacterWalletJournal(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Flux.just(journalResp()));

        List<WalletJournal> result = esiApiService.queryCharacterWalletJournal(CID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(walletApi).queryCharacterWalletJournal(eq(CID), eq("serenity"), eq(1), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    @Test
    @DisplayName("军团钱包流水:未命中 → 真调一次并按 max-age 回填(division 进键)")
    void corpJournal_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/wallets/" + DIVISION + "/journal/", 1);
        when(cacheGateway.get(key)).thenReturn(null);
        when(walletApi.queryCorporationWalletJournalResponse(eq(CORP_ID), eq(DIVISION), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(journalResp()))));

        List<WalletJournal> result = esiApiService.queryCorporationWalletJournal(CORP_ID, DIVISION, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(walletApi).queryCorporationWalletJournalResponse(eq(CORP_ID), eq(DIVISION), eq("serenity"), eq(1), anyString());
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("军团钱包流水:M1 隔离 — division 不同 → 缓存键不同")
    void corpJournalKey_differsByDivision() {
        String k1 = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/corporations/" + CORP_ID + "/wallets/" + DIVISION + "/journal/", 1);
        String k2 = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/corporations/" + CORP_ID + "/wallets/" + (DIVISION + 1) + "/journal/", 1);
        assertThat(k1).isNotEqualTo(k2);
    }

    @Test
    @DisplayName("人物钱包流水 maxPage:缓存命中 → 直接返回,不真调")
    void charJournalMaxPage_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = cacheKeyFor("/characters/" + CID + "/wallet/journal/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(9);

        Integer mp = esiApiService.queryCharacterWalletJournalMaxPage(CID, "Bearer x");

        assertThat(mp).isEqualTo(9);
        verify(walletApi, never()).queryCharacterWalletJournalMaxPage(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("人物钱包流水 maxPage:未命中 → 真调含头入口并按 max-age 回填 Integer")
    void charJournalMaxPage_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/characters/" + CID + "/wallet/journal/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(null);
        when(walletApi.queryCharacterWalletJournalMaxPageResponse(eq(CID), eq("serenity"), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(9, maxAge300()));

        Integer mp = esiApiService.queryCharacterWalletJournalMaxPage(CID, "Bearer x");

        assertThat(mp).isEqualTo(9);
        verify(walletApi).queryCharacterWalletJournalMaxPageResponse(eq(CID), eq("serenity"), anyString());
        verify(cacheGateway).set(eq(key), eq(9), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("军团钱包流水 maxPage:未命中 → 真调含头入口并按 max-age 回填(division 进键)")
    void corpJournalMaxPage_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = cacheKeyFor("/corporations/" + CORP_ID + "/wallets/" + DIVISION + "/journal/", "maxPage");
        when(cacheGateway.get(key)).thenReturn(null);
        when(walletApi.queryCorporationWalletJournalMaxPageResponse(eq(CORP_ID), eq(DIVISION), eq("serenity"), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(6, maxAge300()));

        Integer mp = esiApiService.queryCorporationWalletJournalMaxPage(CORP_ID, DIVISION, "Bearer x");

        assertThat(mp).isEqualTo(6);
        verify(walletApi).queryCorporationWalletJournalMaxPageResponse(eq(CORP_ID), eq(DIVISION), eq("serenity"), anyString());
        verify(cacheGateway).set(eq(key), eq(6), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("人物钱包交易:缓存命中 → 返回缓存数据,不真调")
    void charTransactions_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/characters/" + CID + "/wallet/transactions/?from_id=" + FROM_ID, 0);
        when(cacheGateway.get(key)).thenReturn("[{\"amount\":2}]");

        List<WalletTransaction> result = esiApiService.queryCharacterWalletTransactions(CID, FROM_ID, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(walletApi, never()).queryCharacterWalletTransactionsResponse(any(), anyString(), any(), anyString());
        verify(walletApi, never()).queryCharacterWalletTransactions(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("人物钱包交易:未命中 → 真调一次并按 max-age 回填(from_id 进键)")
    void charTransactions_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/characters/" + CID + "/wallet/transactions/?from_id=" + FROM_ID, 0);
        when(cacheGateway.get(key)).thenReturn(null);
        when(walletApi.queryCharacterWalletTransactionsResponse(eq(CID), eq("serenity"), eq(FROM_ID), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(txnResp()))));

        List<WalletTransaction> result = esiApiService.queryCharacterWalletTransactions(CID, FROM_ID, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(walletApi).queryCharacterWalletTransactionsResponse(eq(CID), eq("serenity"), eq(FROM_ID), anyString());
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("人物钱包交易:未认证 → 直接真调原 Flux 入口,不进缓存")
    void charTransactions_unauthenticated_directRealCall() {
        authAsAnonymous();
        when(walletApi.queryCharacterWalletTransactions(eq(CID), eq("serenity"), eq(FROM_ID), anyString()))
                .thenReturn(Flux.just(txnResp()));

        List<WalletTransaction> result = esiApiService.queryCharacterWalletTransactions(CID, FROM_ID, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(walletApi).queryCharacterWalletTransactions(eq(CID), eq("serenity"), eq(FROM_ID), anyString());
        verify(cacheGateway, never()).get(anyString());
    }

    @Test
    @DisplayName("钱包交易:M1 隔离 — fromId null 与非 null → 缓存键不同")
    void transactionsKey_nullVsNonNullFromId_differs() {
        String base = "/characters/" + CID + "/wallet/transactions/";
        String kNull = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID, base, 0);
        String kNonNull = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                base + "?from_id=" + FROM_ID, 0);
        assertThat(kNull).isNotEqualTo(kNonNull);
    }

    @Test
    @DisplayName("军团钱包交易:未命中 → 真调含头入口并回填(division 与 from_id 进键)")
    void corpTransactions_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID,
                "/corporations/" + CORP_ID + "/wallets/" + DIVISION + "/transactions/?from_id=" + FROM_ID, 0);
        when(cacheGateway.get(key)).thenReturn(null);
        when(walletApi.queryCorporationWalletTransactionsResponse(eq(CORP_ID), eq(DIVISION), eq("serenity"), eq(FROM_ID), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(txnResp()))));

        List<WalletTransaction> result = esiApiService.queryCorporationWalletTransactions(CORP_ID, DIVISION, FROM_ID, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        verify(walletApi).queryCorporationWalletTransactionsResponse(eq(CORP_ID), eq(DIVISION), eq("serenity"), eq(FROM_ID), anyString());
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    // ─═ 015/T012:公开数据包裹(pub 全局共享,不查 UserUtil、不限流) ═─

    private static TypeInfoResponse typeResp() {
        return new TypeInfoResponse();
    }

    private static Id2NameResponse id2Name(int id) {
        Id2NameResponse r = new Id2NameResponse();
        r.setId(id);
        r.setName("name-" + id);
        return r;
    }

    private String pubTypeKey(Integer typeId, String lang) {
        return esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), null,
                "/universe/types/" + typeId + "/" + lang + "/", 0);
    }

    private String pubNamesKey(List<Integer> ids) {
        String encoded = ids.stream().sorted().map(String::valueOf).collect(Collectors.joining("-"));
        return esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), null,
                "/universe/names/" + encoded + "/", 0);
    }

    @Test
    @DisplayName("物品类型 pub:缓存命中 → 返回单值,不真调(公开数据不受用户隔离影响)")
    void universeType_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = pubTypeKey(TYPE_ID, "es");
        when(cacheGateway.get(key)).thenReturn("[{\"typeId\":" + TYPE_ID + "}]");

        InvTypes result = esiApiService.queryUniverseType(TYPE_ID, "es").block();

        assertThat(result).isNotNull();
        verify(universeApi, never()).queryUniverseTypeResponse(any(), anyString(), anyString());
        verify(universeApi, never()).queryUniverseType(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("物品类型 pub:未命中 → 真调一次并按 max-age 回填(单元素列表;userId 段为 pub)")
    void universeType_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = pubTypeKey(TYPE_ID, "es");
        assertThat(key).contains(":pub:").endsWith(":0");
        when(cacheGateway.get(key)).thenReturn(null);
        when(universeApi.queryUniverseTypeResponse(eq(TYPE_ID), eq("serenity"), eq("es")))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(typeResp())));

        InvTypes result = esiApiService.queryUniverseType(TYPE_ID, "es").block();

        assertThat(result).isNotNull();
        verify(universeApi).queryUniverseTypeResponse(eq(TYPE_ID), eq("serenity"), eq("es"));
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("物品类型 pub:全局共享 — 两个不同用户/未认证二次调用命中同一 pub 缓存,只真调一次")
    void universeType_pubSharedAcrossUsers() {
        // 用户 A:未命中 → 真调回填
        authAs(USER_ID);
        String key = pubTypeKey(TYPE_ID, "es");
        when(universeApi.queryUniverseTypeResponse(eq(TYPE_ID), eq("serenity"), eq("es")))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(typeResp())));
        when(cacheGateway.get(key)).thenReturn(null).thenReturn("[{\"typeId\":" + TYPE_ID + "}]");
        // 首请求承担真调需抢到占位锁(FR-008 防穿透)
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        assertThat(esiApiService.queryUniverseType(TYPE_ID, "es").block()).isNotNull();

        // 用户 B(未认证,无 SecurityContext):第二次命中同 pub 键,不真调
        SecurityContextHolder.clearContext();
        assertThat(esiApiService.queryUniverseType(TYPE_ID, "es").block()).isNotNull();

        verify(universeApi, times(1)).queryUniverseTypeResponse(eq(TYPE_ID), eq("serenity"), eq("es"));
    }

    @Test
    @DisplayName("物品类型 pub:M1 隔离 — language 不同 → 缓存键不同")
    void universeTypeKey_differsByLanguage() {
        String zh = pubTypeKey(TYPE_ID, "zh");
        String en = pubTypeKey(TYPE_ID, "en");
        assertThat(zh).isNotEqualTo(en);
    }

    @Test
    @DisplayName("ID→名称 pub:缓存命中 → 返回列表,不真调")
    void universeNames_cacheHit_noRealCall() {
        authAs(USER_ID);
        String key = pubNamesKey(List.of(34, 35));
        when(cacheGateway.get(key)).thenReturn("[{\"name\":\"name-34\"},{\"name\":\"name-35\"}]");

        List<UniverseName> result = esiApiService.queryUniverseNames(List.of(34, 35)).collectList().block();

        assertThat(result).hasSize(2);
        verify(universeApi, never()).queryUniverseNamesResponse(any(), anyString());
        verify(universeApi, never()).queryUniverseNames(any(), anyString());
    }

    @Test
    @DisplayName("ID→名称 pub:未命中 → 真调一次并按 max-age 回填(ids 排序编码进键)")
    void universeNames_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = pubNamesKey(List.of(34, 35));
        assertThat(key).contains(":pub:");
        when(cacheGateway.get(key)).thenReturn(null);
        when(universeApi.queryUniverseNamesResponse(eq(List.of(34, 35)), eq("serenity")))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(id2Name(34), id2Name(35)))));

        List<UniverseName> result = esiApiService.queryUniverseNames(List.of(34, 35)).collectList().block();

        assertThat(result).hasSize(2);
        verify(universeApi).queryUniverseNamesResponse(eq(List.of(34, 35)), eq("serenity"));
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> vc = ArgumentCaptor.forClass(Object.class);
        verify(cacheGateway).set(kc.capture(), vc.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        assertThat(kc.getValue()).isEqualTo(key);
        assertThat(vc.getValue()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("ID→名称 pub:M1 隔离 — ids 组不同 → 缓存键不同")
    void universeNamesKey_differsByIdsGroup() {
        String group1 = pubNamesKey(List.of(34, 35));
        String group2 = pubNamesKey(List.of(34, 36));
        assertThat(group1).isNotEqualTo(group2);
    }

    // ─═ 015/T013:强刷限流接入(forceRefresh 三态) ═─

    @Test
    @DisplayName("强刷:forceRefresh=true 且有额度 → 忽略缓存,真调一次并回填")
    void forceRefresh_true_hasQuota_ignoresCacheAndCalls() {
        authAs(USER_ID);
        String key = assetsKey(1);
        // 预先塞缓存(若无强刷会命中)
        when(cacheGateway.get(key)).thenReturn("[{\"itemId\":100}]");
        when(forceRefreshLimiter.tryAcquire(any(String.class), any(), anyInt())).thenReturn(true);
        // 强刷有额度路径同样经防穿透抢锁(FR-008)
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(999L)))));

        List<Assets> result = esiApiService.queryCharactersAssets(CID, 1, "Bearer x", true).collectList().block();

        // 强刷有额度 → 仍真调(命中缓存被忽略),返回真调数据
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(999L);
        verify(assetsApi).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
        verify(forceRefreshLimiter).tryAcquire(eq(key), any(), anyInt());
    }

    @Test
    @DisplayName("强刷:forceRefresh=true 但超限 → 走缓存命中,不真调")
    void forceRefresh_true_overLimit_returnsCache() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn("[{\"itemId\":100}]");
        when(forceRefreshLimiter.tryAcquire(any(String.class), any(), anyInt())).thenReturn(false);

        List<Assets> result = esiApiService.queryCharactersAssets(CID, 1, "Bearer x", true).collectList().block();

        // 超限 → 落回缓存判读路径,命中返回缓存数据,不真调
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(100L);
        verify(assetsApi, never()).queryCharactersAssetsResponse(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("强刷:默认 forceRefresh=false → 命中缓存不真调,不触碰限流")
    void forceRefresh_false_hitsCache_noLimiterTouch() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn("[{\"itemId\":100}]");

        List<Assets> result = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(100L);
        verify(assetsApi, never()).queryCharactersAssetsResponse(any(), anyString(), any(), anyString());
        verify(forceRefreshLimiter, never()).tryAcquire(any(String.class), any(), anyInt());
    }

    @Test
    @DisplayName("强刷 maxPage:forceRefresh=true 且有额度 → 忽略 getInt 缓存,真调含头入口并回填")
    void forceRefreshMaxPage_true_hasQuota_ignoresCacheAndCalls() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn(12);
        when(forceRefreshLimiter.tryAcquire(any(String.class), any(), anyInt())).thenReturn(true);
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(assetsApi.queryCharactersAssetsMaxPageResponse(eq(CID), eq("serenity"), anyString()))
                .thenReturn(new PageTotalApi.PageCountResult(20, maxAge300()));

        Integer mp = esiApiService.queryCharactersAssetsMaxPage(CID, "Bearer x", true);

        assertThat(mp).isEqualTo(20);
        verify(assetsApi).queryCharactersAssetsMaxPageResponse(eq(CID), eq("serenity"), anyString());
        verify(cacheGateway).set(eq(key), eq(20), eq(300L), eq(TimeUnit.SECONDS));
    }

    // ─═ 015/T013b:FR-008 防并发穿透(未命中 setIfAbsent 串行化真调) ═─

    @Test
    @DisplayName("防穿透:持锁方真调回填 → 释放占位锁(删除 key:lock)")
    void antiPenetration_claimed_releaseClaimAfterBackfill() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(100L)))));

        List<Assets> r = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();

        assertThat(r).hasSize(1);
        verify(assetsApi).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
        // 持锁方回填后必须释放占位锁
        verify(cacheGateway).delete(key + ":lock");
    }

    @Test
    @DisplayName("防穿透:未抢到锁(tryClaim false)→ 有界轮询读缓存命中,不重复真调")
    void antiPenetration_notClaimed_pollHit_noRealCall() {
        authAs(USER_ID);
        String key = assetsKey(1);
        // 初次读缓存未命中;抢锁失败后轮询读到回填结果
        when(cacheGateway.get(key)).thenReturn(null).thenReturn("[{\"itemId\":200}]");
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

        List<Assets> r = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();

        assertThat(r).hasSize(1);
        assertThat(r.get(0).getItemId()).isEqualTo(200L);
        // 未抢到锁且轮询命中 → 复用回填,不触发第二次真调
        verify(assetsApi, never()).queryCharactersAssetsResponse(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("防穿透:未抢到锁且轮询超时 → fail-open fallback 真调一次")
    void antiPenetration_notClaimed_pollTimeout_fallbackRealCall() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(300L)))));

        List<Assets> r = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();

        assertThat(r).hasSize(1);
        assertThat(r.get(0).getItemId()).isEqualTo(300L);
        // 轮询超时按 fail-open 真调一次,不阻塞也不报争用错误
        verify(assetsApi).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
    }

    @Test
    @DisplayName("强刷 L1:force 有额度但未抢锁 → 直接真调,不读陈旧缓存(保 force 新鲜语义)")
    void forceRefresh_notClaimed_doesNotReadStaleCache_usesFreshRealCall() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn("[{\"itemId\":100}]");   // 陈旧缓存
        when(forceRefreshLimiter.tryAcquire(any(String.class), any(), anyInt())).thenReturn(true); // force 有额度
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false); // 未抢锁
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(999L)))));

        List<Assets> r = esiApiService.queryCharactersAssets(CID, 1, "Bearer x", true).collectList().block();

        // force 目标新鲜:即便未抢锁/有陈旧缓存,也要真调取新鲜数据,而非返回 100
        assertThat(r).hasSize(1);
        assertThat(r.get(0).getItemId()).isEqualTo(999L);
        verify(assetsApi).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
    }

    // ─═ 015/Phase3:T014 旅程(SC-001 miss→回填→hit→不真调) ═─

    @Test
    @DisplayName("SC-001 旅程:同一 key 首次 miss → 真调回填,TTL 内二次 hit → 不真调(总真调次数=1)")
    void assets_sameKey_firstMissBackfill_secondHit_noRealCall() {
        authAs(USER_ID);
        String key = assetsKey(1);
        // 首次:缓存未命中;二次:同 key 已回填(缓存命中,T 与 T0 展开见下)
        when(cacheGateway.get(key)).thenReturn(null).thenReturn("[{\"itemId\":500}]");
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(500L)))));

        // 第一次:miss → 真调回填
        List<Assets> first = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();
        assertThat(first).hasSize(1);
        assertThat(first.get(0).getItemId()).isEqualTo(500L);

        // 第二次:同一 key 命中已回填缓存 → 返回缓存值,不真调
        List<Assets> second = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();
        assertThat(second).hasSize(1);
        assertThat(second.get(0).getItemId()).isEqualTo(500L);

        // 全程仅真调一次;第二次走缓存复用
        verify(assetsApi, times(1)).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
    }

    // ─═ 015/Phase3:T016 IDOR 用户隔离(SC-001 重点) ═─

    @Test
    @DisplayName("SC-001 IDOR:两 userId 同一 characterId → 缓存键隔离,各自 miss 真调、互不命中")
    void assets_twoUsers_sameCharacter_isolatedKeys_eachRealCalls() {
        int userB = USER_ID + 1;
        String uri = "/characters/" + CID + "/assets/";
        String keyA = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), USER_ID, uri, 1);
        String keyB = esiResponseCache.cacheKey(esiResponseCacheProperties.getDatasource(), userB, uri, 1);
        // 键必须按 userId 隔离:同 characterId 不同用户不共用键(IDOR 根因,FR-004)
        assertThat(keyA).isNotEqualTo(keyB);

        // 用户 A:未命中 → 真调回填
        authAs(USER_ID);
        when(cacheGateway.get(keyA)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(keyA + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(10L)))));

        List<Assets> a = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();
        assertThat(a).hasSize(1);
        assertThat(a.get(0).getItemId()).isEqualTo(10L);

        // 用户 B 同一 characterId:不同缓存键 → 未命中,A 的回填不使其命中,各自真调
        SecurityContextHolder.clearContext();
        authAs(userB);
        when(cacheGateway.get(keyB)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(keyB + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        List<Assets> b = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();
        assertThat(b).hasSize(1);
        assertThat(b.get(0).getItemId()).isEqualTo(10L);

        // 两个用户各自真调一次(互不共享缓存,IDOR 不成立)
        verify(assetsApi, times(2)).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
        // A/B 回填到各自 userId 的隔离键(而非互相覆盖)
        ArgumentCaptor<String> kc = ArgumentCaptor.forClass(String.class);
        verify(cacheGateway, times(2)).set(kc.capture(), any(), anyLong(), eq(TimeUnit.SECONDS));
        assertThat(kc.getAllValues()).containsExactlyInAnyOrder(keyA, keyB);
    }

    // ─═ 015/Phase3:T018 SC-002 并发不穿透(重点,排队式) ═─

    @Test
    @DisplayName("SC-002 并发不穿透:10 相同已过期键 → 仅首请求抢锁真调回填,其余复用其回填,总真调=1")
    void assets_tenConcurrentSameExpiredKey_onlyOneRealCall() {
        int waiters = 9; // 1 首请求 + 9 等待并发 = 10
        String cached = "[{\"itemId\":100}]";
        authAs(USER_ID);
        String key = assetsKey(1);

        // get 序列:首请求 miss;随后每条等待方 先 miss(初始判读) 再 await 轮询命中已回填缓存
        java.util.List<String> getSeq = new ArrayList<>();
        getSeq.add(null);
        for (int i = 0; i < waiters; i++) {
            getSeq.add(null);
            getSeq.add(cached);
        }
        when(cacheGateway.get(key)).thenReturn(getSeq.get(0), getSeq.subList(1, getSeq.size()).toArray(new String[0]));
        // tryClaim 序列:首请求抢到锁(true),其余等待方未抢到(false) → 轮询复用,不重复真调
        java.util.List<Boolean> claimSeq = new ArrayList<>();
        claimSeq.add(true);
        for (int i = 0; i < waiters; i++) claimSeq.add(false);
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(claimSeq.get(0), claimSeq.subList(1, claimSeq.size()).toArray(new Boolean[0]));
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(100L)))));

        // 首请求:真调回填
        List<Assets> first = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();
        assertThat(first).hasSize(1);

        // 其余并发请求:未抢锁,await 轮询命中已回填缓存,各自返回缓存值
        for (int i = 0; i < waiters; i++) {
            List<Assets> r = esiApiService.queryCharactersAssets(CID, 1, "Bearer x").collectList().block();
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getItemId()).isEqualTo(100L);
        }

        // SC-002 核心:10 并发同键总真调仅 1 次,防穿透成立
        verify(assetsApi, times(1)).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
        verify(assetsApi, never()).queryCharactersAssets(eq(CID), anyString(), eq(1), anyString());
    }

    // ─═ 015/Phase3:T017 SC-003 强刷窗口归零(EsiApiService 侧;limiter 自身计数见其单元测试) ═─

    @Test
    @DisplayName("SC-003 归零:本轮 force 消耗;配额耗尽落缓存;窗口过后 limiter 重新有额度 → force 再真调取新鲜")
    void forceRefresh_windowExpiry_requota_reallowsFresh() {
        authAs(USER_ID);
        String key = assetsKey(1);
        when(cacheGateway.setIfAbsent(eq(key + ":lock"), eq(true), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        // tryAcquire 序列:窗口1 有额度 → 配额耗尽 → 窗口2(过期重新计数)有额度
        when(forceRefreshLimiter.tryAcquire(any(String.class), any(), anyInt())).thenReturn(true, false, true);
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(111L)))));

        // 窗口 1:force 有额度 → 真调回填新鲜的 111
        List<Assets> w1 = esiApiService.queryCharactersAssets(CID, 1, "Bearer x", true).collectList().block();
        assertThat(w1).hasSize(1);
        assertThat(w1.get(0).getItemId()).isEqualTo(111L);

        // 同窗口内配额耗尽(tryAcquire=false)→ 落回缓存命中,返回已回填的 111,不真调
        when(cacheGateway.get(key)).thenReturn("[{\"itemId\":111}]");
        List<Assets> w2 = esiApiService.queryCharactersAssets(CID, 1, "Bearer x", true).collectList().block();
        assertThat(w2).hasSize(1);
        assertThat(w2.get(0).getItemId()).isEqualTo(111L);
        verify(assetsApi, times(1)).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());

        // 窗口 2(过期归零后重新有额度):force 再真调取新鲜 333,忽略陈旧缓存
        when(assetsApi.queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString()))
                .thenReturn(Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(List.of(assetResponse(333L)))));
        List<Assets> w3 = esiApiService.queryCharactersAssets(CID, 1, "Bearer x", true).collectList().block();
        assertThat(w3).hasSize(1);
        assertThat(w3.get(0).getItemId()).isEqualTo(333L);

        verify(assetsApi, times(2)).queryCharactersAssetsResponse(eq(CID), eq("serenity"), eq(1), anyString());
    }
}
