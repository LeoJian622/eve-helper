package xyz.foolcat.eve.evehelper.application.service;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import xyz.foolcat.eve.evehelper.application.dto.request.RefreshTokenRequest;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.TokenResult;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.service.security.RefreshRateLimiterService;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenBlacklistService;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysRoleService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;
import xyz.foolcat.eve.evehelper.shared.kernel.config.JwtTokenProperties;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;

import java.security.KeyPair;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 刷新须校验会话是否已登出 + 轮换拉黑旧 access token(007 T049,T048 派生)。
 *
 * <p><b>缺陷(T048 发现的并发缺口)</b>:T048 让登出撤销 refresh token,但在「刷新 claim 旧 token ->
 * 登出跑完 -> 刷新这才 generate 新 token」的交错下,刷新会凭空产生一个用户不知情的存活 refresh token。
 * T048 的 sid 锚点无法覆盖 -- 撤销发生时该 token 尚未存在。</p>
 *
 * <p><b>T049 修复(A+B,设计评审 BLOCK 后修正)</b>:</p>
 * <ul>
 *   <li><b>A - 会话级失效标记</b>:登出 set {@code session_revoked:<sid>} tombstone(TTL=refresh TTL);
 *       刷新在 claim 旧 refresh token <b>之后</b>、generate <b>之前</b> check tombstone,存在则拒绝。
 *       <b>tombstone 在 logout 中无条件写入,不置于 revokeRefreshTokenBySession 内部</b> -- 该方法 5 条
 *       early-return(索引驱逐/sid 空/token 已不在缓存等)会跳过它,而那恰是攻击者可利用的 fail-open
 *       路径(评审 HIGH-1)。</li>
 *   <li><b>B - 轮换拉黑旧 access jti</b>:{@code generateAccessToken} 写 {@code session_access_jti:<sid> -> <jti>}
 *       (TTL=access TTL);轮换 generate 前 read 旧 jti 拉黑(新增 {@code addToBlacklist(jti, long ttlSeconds)}
 *       重载,评审 HIGH-3),把「轮换后旧 access 残活 15min」降到 0。</li>
 * </ul>
 *
 * <p><b>残留窗口(接受)</b>:刷新 check tombstone(缺)-> 挂起 -> 登出 set tombstone -> 刷新 generate。
 * 亚毫秒级、需精确交错,新 access 最多活 15min。评审确认不可主动拉长(claim 与 check 间无 I/O 暂停点;
 * claim 原子性保证只有一个 refresh 进入 check-generate 区间)。全闭合需 Lua 但 JWT 签名不能进 Lua。</p>
 *
 * <p><b>竞态测试的 hook 位置</b>:挂在 {@code delete(refresh_owner:<uuid>)} -- 这是 refresh「继承 sid 之后、
 * check tombstone 之前」的操作。若挂在 claim-delete(更早),logout 会在 hook 里删掉 refresh_owner,
 * 导致 refresh 读不到 sid 而新建 sid S2,tombstone(在 S1 上)被绕过 -- 那正是 HIGH-2 的机制。</p>
 *
 * <p>用真实 {@link TokenService} + 内存假 {@link CacheGateway}(不 mock TokenService -- 要观察缓存键的真实存亡)。
 * 不依赖 keystore/MySQL/Redis:KeyPair 进程内生成,缓存为 {@link InMemoryCacheGateway}。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@DisplayName("会话撤销闭环(007 T049)")
class SessionRevocationT049Test {

    private static final String REFRESH_KEY_PREFIX = "refresh_token:";
    private static final String SESSION_INDEX_PREFIX = "refresh_session:";
    private static final String SESSION_REVOKED_PREFIX = "session_revoked:";
    private static final String SESSION_ACCESS_JTI_PREFIX = "session_access_jti:";
    private static final Integer USER_ID = 43;
    private static final long REFRESH_TTL_SECONDS = 604800L;
    private static final long ACCESS_TTL_SECONDS = 900L;

    private InMemoryCacheGateway cache;
    private KeyPair keyPair;
    private JwtTokenProperties props;
    private TokenService tokenService;
    private SysUserService sysUserService;
    private SysRoleService sysRoleService;
    private AuthApplicationService authApplicationService;
    private SysUser user;

    @BeforeEach
    void setUp() throws Exception {
        props = new JwtTokenProperties();
        props.setAccessTokenExpirationTime(ACCESS_TTL_SECONDS);
        props.setRefreshTokenExpirationTime(REFRESH_TTL_SECONDS);
        props.setIssuer("eve-helper-test");
        props.setSubject("eve-helper-test");

        keyPair = new RSAKeyGenerator(2048).generate().toKeyPair();

        user = new SysUser();
        user.setId(USER_ID);
        user.setUsername("t049-user");

        sysUserService = Mockito.mock(SysUserService.class);
        sysRoleService = Mockito.mock(SysRoleService.class);

        rebuildServices(new InMemoryCacheGateway());
    }

    /** 用指定缓存重建 tokenService / authApplicationService(竞态用例换用 RaceCacheGateway) */
    private void rebuildServices(InMemoryCacheGateway cacheGateway) {
        this.cache = cacheGateway;
        TokenBlacklistService blacklistService = new TokenBlacklistService(cacheGateway);
        this.tokenService = new TokenService(keyPair, props, cacheGateway, blacklistService);
        this.authApplicationService = new AuthApplicationService(
                blacklistService,
                tokenService,
                sysUserService,
                sysRoleService,
                Mockito.mock(RefreshRateLimiterService.class));
    }

    // ── Concern A: 刷新校验 tombstone ────────────────────────────────────────

    @Test
    @DisplayName("会话已登出(tombstone 已设)-> 刷新必须拒绝,不得生成新 token")
    void refresh_whenSessionRevoked_isRejected() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        stubUserLoad();
        // 模拟「登出已设 tombstone,但 refresh token 仍存活」的竞态瞬间:
        // refresh 已通过存在性校验与 claim,此刻才检查 tombstone
        cache.set(SESSION_REVOKED_PREFIX + sessionIdOf(issued.accessToken()), "1",
                REFRESH_TTL_SECONDS, TimeUnit.SECONDS);

        assertThrows(Exception.class,
                () -> authApplicationService.refreshToken(refreshRequest(issued.refreshToken())),
                "会话已登出(tombstone 已设)却仍能刷新 -- 这是 T048 留下的并发缺口(T049 修复)");
        assertEquals(0, cache.keysWithPrefix(REFRESH_KEY_PREFIX).size(),
                "tombstone 拒绝后不得生成新的 refresh token");
    }

    @Test
    @DisplayName("竞态闭合:刷新继承 sid 后、check tombstone 前登出 set tombstone -> 刷新必须拒绝")
    void refresh_concurrentLogoutSetsTombstone_refreshRejected() {
        RaceCacheGateway raceCache = new RaceCacheGateway();
        rebuildServices(raceCache);
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        stubUserLoad();

        // hook 挂在 delete(refresh_owner:<uuid>):refresh 继承 sid 之后、check tombstone 之前
        raceCache.ownerDeleteKey = "refresh_owner:" + issued.refreshToken();
        raceCache.onOwnerDelete = () -> authApplicationService.logout(requestWith(issued.accessToken()));

        assertThrows(Exception.class,
                () -> authApplicationService.refreshToken(refreshRequest(issued.refreshToken())),
                "刷新 claim 旧 token -> 继承 sid -> [登出 set tombstone] -> check tombstone -- "
                        + "这条 T048 发现的交错必须被 T049 闭合");
        assertFalse(raceCache.containsKey(REFRESH_KEY_PREFIX + issued.refreshToken()),
                "旧 refresh token 应已被 claim 删除");
        assertTrue(raceCache.containsKey(SESSION_REVOKED_PREFIX + sessionIdOf(issued.accessToken())),
                "登出应已设置 tombstone");
        assertEquals(0, raceCache.keysWithPrefix(REFRESH_KEY_PREFIX).size(),
                "tombstone 应阻断新 token 生成 -- 不得留下存活 refresh token");
    }

    // ── Concern A: 登出设置 tombstone(HIGH-1:无条件写入)──────────────────

    @Test
    @DisplayName("登出必须设置 session_revoked:<sid> tombstone")
    void logout_setsSessionRevokedTombstone() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        authApplicationService.logout(requestWith(issued.accessToken()));
        assertTrue(cache.containsKey(SESSION_REVOKED_PREFIX + sessionIdOf(issued.accessToken())),
                "登出必须设置 session_revoked:<sid> -- 否则刷新无法知道会话已登出(T048 的并发缺口)");
    }

    @Test
    @DisplayName("tombstone TTL 必须等于 refresh TTL,不得更短")
    void logout_tombstoneTtlIsRefreshTtl() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        authApplicationService.logout(requestWith(issued.accessToken()));
        assertEquals(REFRESH_TTL_SECONDS, cache.ttlOf(SESSION_REVOKED_PREFIX + sessionIdOf(issued.accessToken())),
                "tombstone TTL 必须 = refresh TTL(604800s)。短于 refresh TTL 则 refresh token 超活 tombstone -> fail-open");
    }

    @Test
    @DisplayName("HIGH-1:索引被驱逐(revoke 返回 false)时 tombstone 仍必须写入")
    void logout_tombstoneSetEvenWhenRevokeReturnsFalse() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        // 模拟 refresh_session:<sid> 被 allkeys-lru 驱逐 -> revokeRefreshTokenBySession 走 early-return(false)
        cache.delete(onlySessionIndexKey());

        authApplicationService.logout(requestWith(issued.accessToken()));

        assertTrue(cache.containsKey(SESSION_REVOKED_PREFIX + sessionIdOf(issued.accessToken())),
                "tombstone 必须在 logout 中无条件写入,不置于 revokeRefreshTokenBySession 内部 -- "
                        + "后者 5 条 early-return(索引驱逐等)会跳过它,而那恰是攻击者可利用的 fail-open 路径(评审 HIGH-1)");
    }

    @Test
    @DisplayName("无 sid(滚动部署遗留)-> 不得设置 tombstone,登出仍须降级为仅拉黑")
    void logout_withoutSid_doesNotSetTombstone() throws Exception {
        String legacyAccessToken = signAccessTokenWithoutSid();
        String jti = tokenService.parseAccessToken(stripBearer(legacyAccessToken)).jti();

        assertDoesNotThrow(() -> authApplicationService.logout(requestWith(legacyAccessToken)),
                "pre-T048 遗留 token(无 sid)登出不得抛异常 -- 降级为仅拉黑");
        assertEquals(0, cache.keysWithPrefix(SESSION_REVOKED_PREFIX).size(),
                "无 sid 时不得设置 tombstone(没有 sid 作键)");
        assertTrue(cache.containsKey(SecurityConstant.TOKEN_BLACKLIST_PREFIX + jti),
                "降级路径下仍必须完成拉黑");
    }

    // ── Concern B: 轮换拉黑旧 access jti ─────────────────────────────────────

    @Test
    @DisplayName("轮换后旧 access token 的 jti 必须被拉黑")
    void rotation_blacklistsOldAccessTokenJti() throws Exception {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        stubUserLoad();
        String oldJti = tokenService.parseAccessToken(stripBearer(issued.accessToken())).jti();

        TokenResult rotated = authApplicationService.refreshToken(refreshRequest(issued.refreshToken()));

        assertNotEquals(oldJti, tokenService.parseAccessToken(stripBearer(rotated.accessToken())).jti(),
                "前置条件:轮换应产生新 access token(新 jti)");
        assertTrue(cache.containsKey(SecurityConstant.TOKEN_BLACKLIST_PREFIX + oldJti),
                "轮换后旧 access token 的 jti 必须被拉黑 -- 否则旧 access 在剩余 TTL 内仍可用(最多 15min)");
    }

    @Test
    @DisplayName("登录后须写入 session_access_jti:<sid> -> <jti>")
    void login_writesSessionAccessJti() throws Exception {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        String jti = tokenService.parseAccessToken(stripBearer(issued.accessToken())).jti();
        String sid = sessionIdOf(issued.accessToken());

        assertEquals(jti, cache.get(SESSION_ACCESS_JTI_PREFIX + sid),
                "登录后须写入 session_access_jti:<sid> -> <jti>,供轮换时拉黑旧 access token");
    }

    @Test
    @DisplayName("session_access_jti TTL 必须等于 access TTL")
    void sessionAccessJti_ttlIsAccessTtl() {
        tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        String sid = onlySessionIndexKey().substring(SESSION_INDEX_PREFIX.length());

        assertEquals(ACCESS_TTL_SECONDS, cache.ttlOf(SESSION_ACCESS_JTI_PREFIX + sid),
                "session_access_jti TTL 必须 = access TTL(900s)-- 与 access token 同寿,过期即无需拉黑");
    }

    @Test
    @DisplayName("session_access_jti 缺失(滚动部署)-> 轮换不得抛异常,降级为跳过拉黑")
    void rotation_withoutSessionAccessJti_degradesGracefully() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        stubUserLoad();
        // 模拟 pre-T049 滚动部署:session_access_jti 键不存在
        cache.delete(SESSION_ACCESS_JTI_PREFIX + sessionIdOf(issued.accessToken()));

        TokenResult rotated = assertDoesNotThrow(
                () -> authApplicationService.refreshToken(refreshRequest(issued.refreshToken())),
                "session_access_jti 缺失(pre-T049 滚动部署)时轮换不得抛异常 -- 降级为跳过拉黑 + SECURITY_ALERT");
        assertNotNull(rotated.refreshToken(), "降级路径下轮换仍须返回新 token");
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void stubUserLoad() {
        Mockito.when(sysUserService.loadUserById(USER_ID)).thenReturn(user);
        Mockito.when(sysRoleService.queryRolesByUserId(USER_ID)).thenReturn(List.of("ROLE_USER"));
    }

    private static RefreshTokenRequest refreshRequest(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);
        return request;
    }

    private String sessionIdOf(String bearerAccessToken) {
        try {
            return tokenService.parseAccessToken(stripBearer(bearerAccessToken)).sessionId();
        } catch (Exception e) {
            throw new IllegalStateException("解析 access token 失败", e);
        }
    }

    private String onlySessionIndexKey() {
        Set<String> keys = cache.keysWithPrefix(SESSION_INDEX_PREFIX);
        if (keys.size() != 1) {
            throw new AssertionError("期望恰好 1 个 " + SESSION_INDEX_PREFIX + "* 键,实际 " + keys.size());
        }
        return keys.iterator().next();
    }

    private String signAccessTokenWithoutSid() {
        return signAccessToken(null);
    }

    private String signAccessToken(String sid) {
        try {
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(props.getSubject())
                    .issuer(props.getIssuer())
                    .jwtID(java.util.UUID.randomUUID().toString())
                    .claim(SecurityConstant.USER_ID_KEY, USER_ID)
                    .claim(SecurityConstant.USER_NAME_KEY, user.getUsername())
                    .claim(SecurityConstant.JWT_AUTHORITIES_KEY, List.of("ROLE_USER"))
                    .expirationTime(new Date(System.currentTimeMillis() + 900_000L));
            if (sid != null) {
                builder.claim(SecurityConstant.SESSION_ID_KEY, sid);
            }
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                    builder.build());
            signedJWT.sign(new RSASSASigner(keyPair.getPrivate()));
            return SecurityConstant.JWT_PREFIX + signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("测试签发 access token 失败", e);
        }
    }

    private HttpServletRequest requestWith(String bearerAccessToken) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(SecurityConstant.AUTHORIZATION_KEY)).thenReturn(bearerAccessToken);
        return request;
    }

    private static String stripBearer(String accessToken) {
        return accessToken.startsWith(SecurityConstant.JWT_PREFIX)
                ? accessToken.substring(SecurityConstant.JWT_PREFIX.length())
                : accessToken;
    }

    /**
     * 内存假 CacheGateway:只实现本测试用到的操作,其余抛 {@link UnsupportedOperationException}。
     * {@code delete} 严格复刻 Redis DEL 语义:只有键真实存在时返回 true。TTL 不做真实过期,但记录供断言。
     */
    static class InMemoryCacheGateway implements CacheGateway {

        final Map<String, Object> store = new ConcurrentHashMap<>();
        final Map<String, Long> ttlSeconds = new ConcurrentHashMap<>();

        volatile boolean deleteReturnsNull;

        boolean containsKey(String key) {
            return store.containsKey(key);
        }

        Long ttlOf(String key) {
            return ttlSeconds.get(key);
        }

        Set<String> keysWithPrefix(String prefix) {
            return store.keySet().stream()
                    .filter(k -> k.startsWith(prefix))
                    .collect(Collectors.toSet());
        }

        @Override
        public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
            store.put(key, value);
            ttlSeconds.put(key, timeUnit.toSeconds(ttl));
        }

        @Override
        public Object get(String key) {
            return store.get(key);
        }

        @Override
        public Boolean setIfAbsent(String key, Object value, long ttl, TimeUnit timeUnit) {
            boolean absent = store.putIfAbsent(key, value) == null;
            if (absent) {
                ttlSeconds.put(key, timeUnit.toSeconds(ttl));
            }
            return absent;
        }

        @Override
        public Boolean delete(String key) {
            ttlSeconds.remove(key);
            boolean removed = store.remove(key) != null;
            return deleteReturnsNull ? null : removed;
        }

        @Override
        public Long delete(Collection<String> keys) {
            return keys.stream().filter(k -> Boolean.TRUE.equals(delete(k))).count();
        }

        @Override
        public Boolean hasKey(String key) {
            return store.containsKey(key);
        }

        @Override
        public Long increment(String key) {
            throw new UnsupportedOperationException("T049 测试未预期 increment 调用");
        }

        @Override
        public Boolean expire(String key, long ttl, TimeUnit timeUnit) {
            throw new UnsupportedOperationException("T049 测试未预期 expire 调用");
        }

        @Override
        public Long getExpire(String key, TimeUnit timeUnit) {
            throw new UnsupportedOperationException("T049 测试未预期 getExpire 调用");
        }

        @Override
        public void putAllHash(String key, Map<String, ?> map) {
            throw new UnsupportedOperationException("T049 测试未预期 putAllHash 调用");
        }

        @Override
        public void convertAndSend(String channel, Object message) {
            throw new UnsupportedOperationException("T049 测试未预期 convertAndSend 调用");
        }
    }

    /**
     * 竞态模拟:在 {@code delete(refresh_owner:<uuid>)} 时同步触发登出,
     * 精确复现「refresh 继承 sid 后、check tombstone 前」与登出交错的时序。
     * hook 一次性(one-shot),避免登出内部对同一键的二次 delete 递归触发。
     */
    static class RaceCacheGateway extends InMemoryCacheGateway {
        String ownerDeleteKey;
        Runnable onOwnerDelete;

        @Override
        public Boolean delete(String key) {
            Boolean result = super.delete(key);
            if (onOwnerDelete != null && key.equals(ownerDeleteKey)) {
                Runnable r = onOwnerDelete;
                onOwnerDelete = null;
                r.run();
            }
            return result;
        }
    }
}
