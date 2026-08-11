package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.CharacterAccessTokenResult;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CharacterApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.EsiAuthStatus;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EsiApiService.getAccessToken 单元测试(纯 Mockito,不依赖 Spring/DB/Redis)。
 * <p>
 * 对应 006 feature 的 Phase 1 加固任务:
 * <ul>
 *     <li>T003(评审 H3 纵深):userId 为空时不写缓存,避免产生跨用户共享的 null 键</li>
 *     <li>T004(评审 C2,FR-013):per-(userId,characterId) 锁 + 5s 超时</li>
 *     <li>T005(评审 M3,FR-019):空值检查不依赖 assert</li>
 *     <li>T014(FR-002~FR-006):越权场景响应不可区分</li>
 * </ul>
 *
 * @author Leojan
 * date 2026-08-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ESI AccessToken 获取单元测试 - 归属校验/锁/超时")
class EsiApiServiceAccessTokenTest {

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
    private static final Integer OTHER_USER_ID = 200;
    private static final Integer CID = 95465499;
    private static final Integer NONEXISTENT_CID = 999999999;
    private static final String ACCESS_TOKEN_KEY = "esi_access_token:" + USER_ID + ":" + CID;
    /**
     * 刷新锁按 characterId(与 getAuthorizationStatus 共用),不含 userId:
     * refreshToken 存在 eve_account 的角色行上,是"每角色一份"的资源。
     */
    private static final String REFRESH_LOCK_KEY = "esi_refresh_lock:" + CID;
    /**
     * TTL 不可用(null/-1/-2)时的回退值 = 缓存 TTL 19 分钟。
     * 不回退为 0:0 会被客户端读作"已过期",诱发对刚取得的有效 token 立即重取。
     */
    private static final long TTL_FALLBACK_SECONDS = 19 * 60;

    /**
     * 构造含 userId/characterId/refreshToken 的测试角色。
     */
    private EveAccount account(String refreshToken) {
        EveAccount a = new EveAccount();
        a.setUserId(USER_ID);
        a.setCharacterId(CID);
        a.setRefreshToken(refreshToken);
        return a;
    }

    /**
     * 归属校验失败:getAccountOne 走 SQL `where user_id=? and character_id=?`,
     * 无匹配行时抛 USER_ACCOUNT_NOT_EXIST。
     */
    private EveHelperException notExist() {
        return new EveHelperException(
                xyz.foolcat.eve.evehelper.shared.result.ResultCode.USER_ACCOUNT_NOT_EXIST);
    }

    // ── T004: 缓存命中路径 ──

    @Test
    @DisplayName("缓存命中 -> 直接返回缓存值,不调用 ESI,不加锁")
    void cacheHit_returnsCachedTokenWithoutEsiCall() throws Exception {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn("Bearer cached-at");

        String token = esiApiService.getAccessToken(CID, USER_ID);

        assertEquals("Bearer cached-at", token);
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
        verify(cacheGateway, never()).setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    // ── T004: 锁语义(FR-013,评审 C2) ──

    @Test
    @DisplayName("抢不到锁且等待窗口内缓存未被填充 -> 抛 EsiException,不调用 ESI 刷新")
    void lockNotAcquired_cacheNeverFilled_throwsWithoutRefresh() {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(REFRESH_LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        // ESI refreshToken 一次性使用:并发刷新会使一方用已失效 token,
        // 且竞态回写可能落库已作废值导致绑定永久失效,故抢不到锁绝不可硬刷
        assertThrows(EsiException.class, () -> esiApiService.getAccessToken(CID, USER_ID));
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
    }

    @Test
    @DisplayName("setIfAbsent 返回 null(Redis 异常) -> 视为未获得锁,绝不落入刷新(fail-closed)")
    void lockResultNull_treatedAsNotAcquired() {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn(null);
        // Redis 异常或 RedisTemplate 返回 null 时,setIfAbsent 返回 null。
        // 若判断写成 Boolean.FALSE.equals(acquired),null 会被当作"已持锁"而落入刷新 —— 锁形同虚设。
        when(cacheGateway.setIfAbsent(eq(REFRESH_LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(null);

        assertThrows(EsiException.class, () -> esiApiService.getAccessToken(CID, USER_ID));
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
    }

    @Test
    @DisplayName("抢不到锁但持锁方随后填充缓存 -> 读缓存返回,不报错也不重复刷新")
    void lockNotAcquired_cacheFilledByWinner_returnsCachedToken() throws Exception {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        // 首次读未命中 -> 抢锁失败 -> 轮询期间持锁方写入缓存
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn(null).thenReturn("Bearer filled-by-winner");
        when(cacheGateway.setIfAbsent(eq(REFRESH_LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(false);
        when(cacheGateway.getExpire(ACCESS_TOKEN_KEY, TimeUnit.SECONDS)).thenReturn(900L);

        String result = esiApiService.getAccessToken(CID, USER_ID);

        // 争用不是故障:持锁方已完成刷新,等待方应读到结果而非收到 ESI 服务异常
        assertEquals("Bearer filled-by-winner", result);
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
    }

    @Test
    @DisplayName("持锁刷新过程中异常 -> 仍在 finally 释放锁,不留下死锁")
    void lockAcquired_refreshFails_stillReleasesLock() {
        AuthTokenResponse token = new AuthTokenResponse();
        // accessToken 非合法 JWT:updateRefreshToken 内的 SignedJWT.parse 会抛 ParseException。
        // 本用例验证的是「异常路径下锁仍被释放」——锁泄漏会让该角色在 TTL 内无法再刷新。
        token.setAccessToken("not-a-jwt");
        token.setRefreshToken("new-rt");
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("old-rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(REFRESH_LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "old-rt")).thenReturn(Mono.just(token));

        // SignedJWT.parse("not-a-jwt") 经 @SneakyThrows 抛出 ParseException
        assertThrows(java.text.ParseException.class, () -> esiApiService.getAccessToken(CID, USER_ID));

        verify(cacheGateway).delete(REFRESH_LOCK_KEY);
    }

    @Test
    @DisplayName("锁已被他人接管(owner 不匹配) -> 跳过删除,不误释放别人的锁")
    void lockTakenOverByOther_skipsRelease() {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(REFRESH_LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        // 临界区耗时超过锁 TTL,锁过期后被他人重新获得 —— 此时读到的是别人的 owner
        when(cacheGateway.get(REFRESH_LOCK_KEY)).thenReturn("someone-elses-owner-token");
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt")).thenReturn(Mono.empty());

        assertThrows(EsiException.class, () -> esiApiService.getAccessToken(CID, USER_ID));

        // 无条件 delete 会摧毁新持有者的锁,使其临界区失去保护
        verify(cacheGateway, never()).delete(REFRESH_LOCK_KEY);
    }

    @Test
    @DisplayName("持锁后 double-check 命中缓存 -> 不重复刷新")
    void lockAcquired_doubleCheckHit_skipsRefresh() throws Exception {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        // 按键分别 stub,避免依赖 get 的调用顺序(releaseRefreshLock 也会读锁键)
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn(null).thenReturn("Bearer filled-by-other");
        when(cacheGateway.setIfAbsent(eq(REFRESH_LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(cacheGateway.getExpire(ACCESS_TOKEN_KEY, TimeUnit.SECONDS)).thenReturn(900L);

        String result = esiApiService.getAccessToken(CID, USER_ID);

        assertEquals("Bearer filled-by-other", result);
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
        verify(cacheGateway).delete(REFRESH_LOCK_KEY);
    }

    // ── T005: 空值检查不依赖 assert(FR-019,评审 M3) ──

    @Test
    @DisplayName("ESI 返回空响应 -> 抛 EsiException 而非 NPE(assert 在生产环境为空语句)")
    void nullAuthTokenResponse_throwsEsiException() {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(REFRESH_LOCK_KEY), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt")).thenReturn(Mono.empty());

        assertThrows(EsiException.class, () -> esiApiService.getAccessToken(CID, USER_ID));
        verify(cacheGateway).delete(REFRESH_LOCK_KEY);
    }

    // ── T014: 越权场景不可区分(FR-002~FR-006,SC-007) ──

    @Test
    @DisplayName("角色属他人 -> 抛 ESI_AUTHORIZATION_FAILURE,不泄露账户存在性")
    void characterOwnedByOtherUser_throwsAuthorizationFailure() {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenThrow(notExist());

        EsiException ex = assertThrows(EsiException.class, () -> esiApiService.getAccessToken(CID, USER_ID));

        assertEquals(ResultCode.ESI_AUTHORIZATION_FAILURE, ex.getResultCode());
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
    }

    @Test
    @DisplayName("角色不存在 -> 错误码与消息必须与'属他人'完全一致(消除存在性 oracle)")
    void nonexistentCharacter_indistinguishableFromOwnedByOther() {
        when(eveAccountService.getAccountOne(USER_ID, NONEXISTENT_CID)).thenThrow(notExist());
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenThrow(notExist());

        EsiException nonexistent =
                assertThrows(EsiException.class, () -> esiApiService.getAccessToken(NONEXISTENT_CID, USER_ID));
        EsiException ownedByOther =
                assertThrows(EsiException.class, () -> esiApiService.getAccessToken(CID, USER_ID));

        // 两者必须完全不可区分,否则构成"某角色是否存在"的枚举 oracle
        assertEquals(ownedByOther.getResultCode(), nonexistent.getResultCode());
        assertEquals(ownedByOther.getMessage(), nonexistent.getMessage());
    }

    @Test
    @DisplayName("归属校验用传入 userId 而非 SecurityContext,不同用户查同一角色互相隔离")
    void ownershipUsesPassedUserId_notSecurityContext() {
        // 同一 characterId,不同 userId 各自独立查库;OTHER_USER_ID 无该角色
        when(eveAccountService.getAccountOne(OTHER_USER_ID, CID)).thenThrow(notExist());

        assertThrows(EsiException.class, () -> esiApiService.getAccessToken(CID, OTHER_USER_ID));

        verify(eveAccountService).getAccountOne(OTHER_USER_ID, CID);
        verify(eveAccountService, never()).getAccountOne(USER_ID, CID);
    }

    @Test
    @DisplayName("ROOT/ADMIN 主体访问非本人角色 -> 仍被拒绝(ROOT 不豁免归属校验)")
    void rootAdminPrincipal_stillDeniedForForeignCharacter() {
        // 在安全上下文中放入带 ADMIN 权限的已认证主体。
        // AccessGuard.requireOwnership 会对 ROOT 短路豁免(AccessGuard:44-46),
        // 本路径刻意绕开该守卫改走 SQL 精确匹配,故 ADMIN 不应获得任何额外通行权:
        // accessToken 是可直接冒用的凭证,ADMIN 令牌泄露不得等于全站角色接管。
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                    USER_ID, "n/a", List.of(new SimpleGrantedAuthority(GlobalConstants.ROOT_ROLE_CODE)));
            SecurityContext adminContext = SecurityContextHolder.createEmptyContext();
            adminContext.setAuthentication(adminAuth);
            SecurityContextHolder.setContext(adminContext);

            when(eveAccountService.getAccountOne(USER_ID, CID)).thenThrow(notExist());

            EsiException ex = assertThrows(EsiException.class, () -> esiApiService.getAccessToken(CID, USER_ID));

            assertEquals(ResultCode.ESI_AUTHORIZATION_FAILURE, ex.getResultCode());
            verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    @Test
    @DisplayName("同军团他人角色 -> 拒绝(corpId 不构成 token 访问授权)")
    void sameCorporationForeignCharacter_denied() {
        // corp 维度授权在其它查询接口是允许的(ResourceOwnershipPolicy 会按 corpId 放行),
        // 但对 token 不可接受:攻击者用小号加入目标军团即可窃取团内他人凭证。
        // 本路径只查 (userId, characterId),同团他人角色在该 SQL 下必然无匹配行。
        Integer foreignCharacterInSameCorp = 91000001;
        when(eveAccountService.getAccountOne(USER_ID, foreignCharacterInSameCorp)).thenThrow(notExist());

        EsiException ex = assertThrows(EsiException.class,
                () -> esiApiService.getAccessToken(foreignCharacterInSameCorp, USER_ID));

        assertEquals(ResultCode.ESI_AUTHORIZATION_FAILURE, ex.getResultCode());
        verify(authorizeOAuth, never()).updateAccessToken(any(), anyString());
    }

    // ── T009: getAccessTokenWithExpiry 的 expiresIn 语义(FR-018,评审 M1) ──

    @Test
    @DisplayName("缓存命中 -> expiresIn 取 Redis 剩余 TTL")
    void withExpiry_cacheHit_usesRedisTtl() throws Exception {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn("Bearer cached-at");
        when(cacheGateway.getExpire(ACCESS_TOKEN_KEY, TimeUnit.SECONDS)).thenReturn(842L);

        CharacterAccessTokenResult result = esiApiService.getAccessTokenWithExpiry(CID, USER_ID);

        assertEquals("Bearer cached-at", result.accessToken());
        assertEquals(CID, result.characterId());
        assertEquals(842L, result.expiresIn());
    }

    @Test
    @DisplayName("TTL 返回哨兵值 -2(键不存在) -> 归一化为 0,不透出哨兵值")
    void withExpiry_ttlKeyMissingSentinel_normalizedToZero() throws Exception {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn("Bearer cached-at");
        // CacheGateway 约定:键不存在返回 -2
        when(cacheGateway.getExpire(ACCESS_TOKEN_KEY, TimeUnit.SECONDS)).thenReturn(-2L);

        CharacterAccessTokenResult result = esiApiService.getAccessTokenWithExpiry(CID, USER_ID);

        assertEquals(TTL_FALLBACK_SECONDS, result.expiresIn(), "哨兵值 -2 须回退为缓存 TTL,不得透出 0(会被读作已过期)");
    }

    @Test
    @DisplayName("TTL 返回哨兵值 -1(无 TTL) -> 归一化为 0")
    void withExpiry_ttlNoExpirySentinel_normalizedToZero() throws Exception {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn("Bearer cached-at");
        // CacheGateway 约定:无 TTL 返回 -1
        when(cacheGateway.getExpire(ACCESS_TOKEN_KEY, TimeUnit.SECONDS)).thenReturn(-1L);

        CharacterAccessTokenResult result = esiApiService.getAccessTokenWithExpiry(CID, USER_ID);

        assertEquals(TTL_FALLBACK_SECONDS, result.expiresIn(), "哨兵值 -1 须回退为缓存 TTL,不得透出 0");
    }

    @Test
    @DisplayName("TTL 返回 null -> 归一化为 0,不抛 NPE")
    void withExpiry_ttlNull_normalizedToZero() throws Exception {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenReturn(account("rt"));
        when(cacheGateway.get(ACCESS_TOKEN_KEY)).thenReturn("Bearer cached-at");
        when(cacheGateway.getExpire(ACCESS_TOKEN_KEY, TimeUnit.SECONDS)).thenReturn(null);

        CharacterAccessTokenResult result = esiApiService.getAccessTokenWithExpiry(CID, USER_ID);

        assertEquals(TTL_FALLBACK_SECONDS, result.expiresIn());
    }

    @Test
    @DisplayName("归属校验失败 -> getAccessTokenWithExpiry 与 getAccessToken 抛同样的异常")
    void withExpiry_ownershipFailure_throwsSameAsPlainGetter() {
        when(eveAccountService.getAccountOne(USER_ID, CID)).thenThrow(notExist());

        EsiException ex =
                assertThrows(EsiException.class, () -> esiApiService.getAccessTokenWithExpiry(CID, USER_ID));

        assertEquals(ResultCode.ESI_AUTHORIZATION_FAILURE, ex.getResultCode());
    }

    // ── T003: userId 为空时不写缓存(评审 H3 纵深) ──

    @Test
    @DisplayName("account.userId 为 null -> 不写 accessToken 缓存,避免产生跨用户共享的 null 键")
    void nullUserIdInAccount_skipsCacheWrite() {
        EveAccount noUserId = new EveAccount();
        noUserId.setCharacterId(CID);
        noUserId.setRefreshToken("rt");
        // userId 未设置(模拟 queryAccountList 漏 select user_id 的历史行为)
        AuthTokenResponse token = new AuthTokenResponse();
        token.setAccessToken("at");
        token.setRefreshToken("new-rt");
        when(cacheGateway.get("esi_auth_status:" + CID)).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq("esi_refresh_lock:" + CID), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, "rt")).thenReturn(Mono.just(token));

        EsiAuthStatus status = esiApiService.getAuthorizationStatus(noUserId);

        assertEquals(EsiAuthStatus.AUTHORIZED, status);
        // 关键断言:不得写入 esi_access_token:null:{cid}
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheGateway, Mockito.atLeastOnce())
                .set(keyCaptor.capture(), any(), anyLong(), eq(TimeUnit.SECONDS));
        keyCaptor.getAllValues().forEach(key ->
                assertFalse(key.contains(":null:"),
                        "缓存键不得含 null userId,否则多个用户共享同一 accessToken 键:" + key));
    }
}
