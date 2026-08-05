package xyz.foolcat.eve.evehelper.domain.service.esi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CharacterApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ESI 授权状态判定单元测试(Mockito,不依赖 Spring 上下文)。
 *
 * @author Leojan
 * date 2026-08-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ESI 授权状态判定单元测试")
class EsiApiServiceUnitTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @Mock
    EveAccountService eveAccountService;

    @Mock
    AuthorizeOAuth authorizeOAuth;

    @Mock
    CharacterApi characterApi;

    @Mock
    UniverseApi universeApi;

    @Mock
    AuthorizeUtil authorizeUtil;

    @InjectMocks
    EsiApiService esiApiService;

    private static final Integer CID = 95465499;
    private static final String STATUS_KEY = "esi_auth_status:95465499";
    private static final String LOCK_KEY = "esi_auth_status_lock:95465499";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private EveAccount account(String refreshToken) {
        EveAccount a = new EveAccount();
        a.setCharacterId(CID);
        a.setRefreshToken(refreshToken);
        return a;
    }

    /**
     * 模拟缓存未命中 + 成功获取 per-character 锁。
     */
    private void stubCacheMissAndLockAcquired() {
        when(valueOperations.get(STATUS_KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
    }

    @Test
    @DisplayName("refreshToken 为空 -> NOT_AUTHORIZED,不触发 ESI/缓存/锁")
    void blankRefreshToken_returnsNotAuthorized() {
        assertEquals(EsiAuthStatus.NOT_AUTHORIZED, esiApiService.getAuthorizationStatus(account("")));
        assertEquals(EsiAuthStatus.NOT_AUTHORIZED, esiApiService.getAuthorizationStatus(account(null)));
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("刷新成功 -> AUTHORIZED,回写新 refreshToken 并缓存 accessToken")
    void refreshSuccess_returnsAuthorized() {
        EveAccount acc = account("old-rt");
        AuthTokenResponse token = new AuthTokenResponse();
        token.setAccessToken("at");
        token.setRefreshToken("new-rt");
        stubCacheMissAndLockAcquired();
        when(eveAccountService.insertOrUpdateSelective(any(EveAccount.class))).thenReturn(1);
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "old-rt")).thenReturn(Mono.just(token));

        EsiAuthStatus status = esiApiService.getAuthorizationStatus(acc);

        assertEquals(EsiAuthStatus.AUTHORIZED, status);
        verify(valueOperations).set(eq("esi_access_token:95465499"), eq("Bearer at"), eq(1140L), eq(TimeUnit.SECONDS));
        ArgumentCaptor<EveAccount> captor = ArgumentCaptor.forClass(EveAccount.class);
        verify(eveAccountService).insertOrUpdateSelective(captor.capture());
        assertEquals(CID, captor.getValue().getCharacterId());
        assertEquals("new-rt", captor.getValue().getRefreshToken());
        verify(valueOperations).set(eq(STATUS_KEY), eq("AUTHORIZED"), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("刷新成功但 refreshToken 未变化 -> 跳过 DB 回写")
    void refreshSuccess_unchangedToken_skipsUpdate() {
        EveAccount acc = account("same-rt");
        AuthTokenResponse token = new AuthTokenResponse();
        token.setAccessToken("at");
        token.setRefreshToken("same-rt");
        stubCacheMissAndLockAcquired();
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "same-rt")).thenReturn(Mono.just(token));

        assertEquals(EsiAuthStatus.AUTHORIZED, esiApiService.getAuthorizationStatus(acc));
        verify(eveAccountService, never()).insertOrUpdateSelective(any(EveAccount.class));
    }

    @Test
    @DisplayName("updateAccessToken 成功但 accessToken 为空 -> UNKNOWN,不缓存 accessToken")
    void blankAccessToken_returnsUnknown_skipsAccessTokenCache() {
        EveAccount acc = account("old-rt");
        AuthTokenResponse token = new AuthTokenResponse();
        token.setAccessToken("");
        token.setRefreshToken("new-rt");
        stubCacheMissAndLockAcquired();
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "old-rt")).thenReturn(Mono.just(token));

        assertEquals(EsiAuthStatus.UNKNOWN, esiApiService.getAuthorizationStatus(acc));
        verify(valueOperations, never()).set(eq("esi_access_token:95465499"), anyString(), anyLong(), eq(TimeUnit.SECONDS));
        verify(eveAccountService, never()).insertOrUpdateSelective(any(EveAccount.class));
    }

    @Test
    @DisplayName("ESI_AUTHORIZATION_FAILURE(4xx) -> EXPIRED,不回写 token")
    void refresh4xx_returnsExpired() {
        EveAccount acc = account("rt");
        stubCacheMissAndLockAcquired();
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt"))
                .thenReturn(Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE)));

        assertEquals(EsiAuthStatus.EXPIRED, esiApiService.getAuthorizationStatus(acc));
        verify(eveAccountService, never()).insertOrUpdateSelective(any(EveAccount.class));
    }

    @Test
    @DisplayName("ESI_SERVER_FAILURE(5xx) -> UNKNOWN")
    void refresh5xx_returnsUnknown() {
        EveAccount acc = account("rt");
        stubCacheMissAndLockAcquired();
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt"))
                .thenReturn(Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE)));

        assertEquals(EsiAuthStatus.UNKNOWN, esiApiService.getAuthorizationStatus(acc));
    }

    @Test
    @DisplayName("超时(TimeoutException) -> UNKNOWN")
    void refreshTimeout_returnsUnknown() {
        EveAccount acc = account("rt");
        stubCacheMissAndLockAcquired();
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt"))
                .thenReturn(Mono.error(new TimeoutException()));

        assertEquals(EsiAuthStatus.UNKNOWN, esiApiService.getAuthorizationStatus(acc));
    }

    @Test
    @DisplayName("状态缓存命中 -> 直接返回,不调用 ESI,不加锁")
    void cacheHit_returnsCached() {
        EveAccount acc = account("rt");
        when(valueOperations.get(STATUS_KEY)).thenReturn("EXPIRED");

        assertEquals(EsiAuthStatus.EXPIRED, esiApiService.getAuthorizationStatus(acc));
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("缓存值为非法字符串 -> 回退重新判定为 AUTHORIZED")
    void cacheInvalidString_fallsThroughToRefresh() {
        EveAccount acc = account("rt");
        AuthTokenResponse token = new AuthTokenResponse();
        token.setAccessToken("at");
        token.setRefreshToken("new-rt");
        when(valueOperations.get(STATUS_KEY)).thenReturn("BOGUS");
        when(eveAccountService.insertOrUpdateSelective(any(EveAccount.class))).thenReturn(1);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt")).thenReturn(Mono.just(token));

        assertEquals(EsiAuthStatus.AUTHORIZED, esiApiService.getAuthorizationStatus(acc));
    }

    @Test
    @DisplayName("未获取到锁(并发) -> 返回 UNKNOWN,不调用 ESI")
    void lockNotAcquired_returnsUnknownWithoutRefresh() {
        EveAccount acc = account("rt");
        when(valueOperations.get(STATUS_KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(false);

        assertEquals(EsiAuthStatus.UNKNOWN, esiApiService.getAuthorizationStatus(acc));
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
    }
}
