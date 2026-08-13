package xyz.foolcat.eve.evehelper.domain.service.esi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CharacterApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
@ExtendWith(MockitoExtension.class)
@DisplayName("ESI 授权状态判定单元测试 - 四态 + 缓存")
class EsiApiServiceTest {

    @Mock
    CacheGateway cacheGateway;

    @Mock
    EveAccountService eveAccountService;

    @Mock
    AuthorizeOAuth authorizeOAuth;

    @Mock
    CharacterApi characterApi;

    @Mock
    UniverseApi universeApi;

    @InjectMocks
    EsiApiService esiApiService;

    private static final Integer USER_ID = 100;
    private static final Integer CID = 95465499;
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
}
