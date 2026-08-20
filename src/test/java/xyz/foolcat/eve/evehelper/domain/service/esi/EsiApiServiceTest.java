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
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCache;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache.EsiResponseCacheProperties;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AssetResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.IndustryJobPlacedResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ObserverMiningLedgerResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
        String key = assetsKey(1);
        when(cacheGateway.get(key)).thenReturn(20);

        Integer mp = esiApiService.queryCharactersAssetsMaxPage(CID, "Bearer x");

        assertThat(mp).isEqualTo(20);
        verify(assetsApi, never()).queryCharactersAssetsMaxPage(any(), anyString(), anyString());
        verify(assetsApi, never()).queryCharactersAssetsMaxPageResponse(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("资产 maxPage:未命中 → 真调含头入口并按 max-age 回填 Integer")
    void characterAssetsMaxPage_cacheMiss_realCallAndBackfill() {
        authAs(USER_ID);
        String key = assetsKey(1);
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
        String key = assetsKey(1);
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
}
