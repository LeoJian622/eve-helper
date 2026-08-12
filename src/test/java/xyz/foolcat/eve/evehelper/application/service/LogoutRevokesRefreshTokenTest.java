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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * 登出必须撤销 refresh token(007 T048,T042 派生发现)。
 *
 * <p><b>缺陷</b>:{@code AuthApplicationService.logout} 只把 access token 的 {@code jti}
 * 加入黑名单,<b>从不撤销 refresh token</b>。access token TTL 900s、refresh token TTL
 * 604800s(7 天),于是:</p>
 * <pre>
 *   用户点「登出」 → access token 被拉黑(15 分钟内失效)
 *                 → refresh_token:&lt;uuid&gt; 仍在 Redis 中完好无损
 *   攻击者持该 refresh token → POST /auth/tokens → 换到<b>全新</b> token 对
 *                            → 登出形同虚设,凭证实际存活 7 天
 * </pre>
 *
 * <p><b>⚠️ 锚点必须是会话标识 {@code sid},不得是 {@code jti}</b>(设计评审 CRITICAL)。
 * 初版设计以 access token 的 {@code jti} 作索引键 {@code refresh_index:<jti>}。但
 * <b>jti 每次轮换都会变,且轮换不拉黑旧 access token</b>,而 {@code POST /auth/tokens}
 * 在白名单 —— 攻击者持被盗 refresh token 发<b>一个未认证请求</b>即可让索引永久错位:
 * 此后受害者每次登出都撤不到当前 refresh token,<b>却照样返回 204 成功</b>。
 * 即控制项在它唯一的存在理由(凭证已泄露)下失效。故锚点改为登录时生成、
 * <b>轮换时原样继承</b>的 {@code sid},索引 {@code refresh_session:<sid> -> <uuid>}
 * 在轮换时<b>重写</b>指向新 uuid。见 {@link #rotateThenLogout_revokesCurrentRefreshToken()}。</p>
 *
 * <p><b>为何用真实 {@link TokenService} 而非 mock</b>:本类要证明的是
 * <b>缓存中的键是否真被删除</b> —— 这是 TokenService 与 CacheGateway 协作的结果。
 * 若把 TokenService mock 掉(如 {@code AuthApplicationServiceUnitTest} 所做),
 * 断言只能验证「某方法被调用」,而 T048 的漏洞恰恰在于<b>压根没有这样一个方法</b>,
 * mock 断言便无从下手。故此处用内存假 CacheGateway 承载真实读写,
 * 直接观察 {@code refresh_token:&lt;uuid&gt;} 的存亡,并实际再刷一次验证可利用性。</p>
 *
 * <p>不依赖 keystore、不依赖 MySQL、不依赖 Redis:KeyPair 进程内生成,缓存为
 * {@link InMemoryCacheGateway}。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@DisplayName("登出撤销 Refresh Token(007 T048)")
class LogoutRevokesRefreshTokenTest {

    private static final String REFRESH_KEY_PREFIX = "refresh_token:";
    private static final String SESSION_INDEX_PREFIX = "refresh_session:";
    private static final Integer USER_ID = 42;
    private static final long REFRESH_TTL_SECONDS = 604800L;

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
        cache = new InMemoryCacheGateway();

        props = new JwtTokenProperties();
        props.setAccessTokenExpirationTime(900L);
        props.setRefreshTokenExpirationTime(REFRESH_TTL_SECONDS);
        props.setIssuer("eve-helper-test");
        props.setSubject("eve-helper-test");

        keyPair = new RSAKeyGenerator(2048).generate().toKeyPair();
        tokenService = new TokenService(keyPair, props, cache);

        user = new SysUser();
        user.setId(USER_ID);
        user.setUsername("t048-user");

        sysUserService = Mockito.mock(SysUserService.class);
        sysRoleService = Mockito.mock(SysRoleService.class);

        authApplicationService = new AuthApplicationService(
                new TokenBlacklistService(cache),
                tokenService,
                sysUserService,
                sysRoleService,
                Mockito.mock(RefreshRateLimiterService.class));
    }

    // ── 基本撤销 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("登出后 refresh token 必须已从缓存中撤销")
    void logout_revokesRefreshToken() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        String refreshKey = REFRESH_KEY_PREFIX + issued.refreshToken();
        assertNotNull(cache.get(refreshKey), "前置条件:签发后 refresh token 应存在于缓存");

        authApplicationService.logout(requestWith(issued.accessToken()));

        assertFalse(cache.containsKey(refreshKey),
                "登出后 refresh token 仍在缓存中 —— 持该 token 者可继续换取新 token 对,"
                        + "登出形同虚设(access token 15 分钟失效,refresh 却有 7 天)(T048)");
    }

    @Test
    @DisplayName("可利用性:登出后再用该 refresh token 刷新必须失败")
    void logout_thenRefresh_isRejected() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        stubUserLoad();

        authApplicationService.logout(requestWith(issued.accessToken()));

        assertThrows(Exception.class, () -> authApplicationService.refreshToken(refreshRequest(issued.refreshToken())),
                "登出后仍能用旧 refresh token 换到新 token 对 —— 这是 T048 漏洞的直接利用路径");
    }

    @Test
    @DisplayName("登出仍必须拉黑 access token 的 jti(不得因新增撤销而回退)")
    void logout_stillBlacklistsAccessToken() throws Exception {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        String jti = tokenService.parseAccessToken(stripBearer(issued.accessToken())).jti();

        authApplicationService.logout(requestWith(issued.accessToken()));

        assertTrue(cache.containsKey(SecurityConstant.TOKEN_BLACKLIST_PREFIX + jti),
                "access token 的 jti 必须仍被拉黑 —— 撤销 refresh token 是新增行为,不是替换");
    }

    @Test
    @DisplayName("登出同时删除会话索引自身,不留悬垂键")
    void logout_deletesSessionIndex() {
        tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        String indexKey = onlySessionIndexKey();

        authApplicationService.logout(requestWith(reissueAccessTokenFor(indexKey)));

        assertFalse(cache.containsKey(indexKey),
                "会话索引键未随登出删除 —— 索引值是 refresh token 明文,"
                        + "悬垂键会让已撤销会话的凭证在缓存中多留最长 7 天");
    }

    // ── 锚点正确性(评审 CRITICAL)────────────────────────────────────────

    @Test
    @DisplayName("锚点锁:攻击者先轮换,受害者用旧 access token 登出仍须撤销当前 refresh token")
    void rotateThenLogout_revokesCurrentRefreshToken() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        stubUserLoad();

        // 攻击者持被盗 refresh token 发一个未认证请求(POST /auth/tokens 在白名单)
        TokenResult rotated = authApplicationService.refreshToken(refreshRequest(issued.refreshToken()));
        assertNotEquals(issued.refreshToken(), rotated.refreshToken(), "前置条件:轮换应产生新 refresh token");

        // 受害者用「轮换前」的 access token 登出 —— 轮换不拉黑旧 access token,故它仍可用
        authApplicationService.logout(requestWith(issued.accessToken()));

        assertFalse(cache.containsKey(REFRESH_KEY_PREFIX + rotated.refreshToken()),
                "受害者登出后,轮换产生的<b>最新</b> refresh token 仍然存活 —— "
                        + "说明索引锚点随轮换失效(用 jti 作锚点即如此):攻击者只需发一个未认证的刷新请求,"
                        + "此后受害者每次登出都撤不到当前凭证却照样返回 204 成功。锚点必须是轮换不变的 sid(T048 CRITICAL)");
    }

    @Test
    @DisplayName("轮换必须把会话索引重写为新的 refresh token")
    void rotation_rewritesSessionIndex() {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        stubUserLoad();
        String indexKeyBefore = onlySessionIndexKey();

        TokenResult rotated = authApplicationService.refreshToken(refreshRequest(issued.refreshToken()));

        assertEquals(indexKeyBefore, onlySessionIndexKey(),
                "轮换后会话索引键(sid)必须不变 —— 变了就说明 sid 未被继承,锚点会再次错位");
        assertEquals(rotated.refreshToken(), cache.get(indexKeyBefore),
                "轮换后会话索引必须指向新的 refresh token,否则登出撤销的是已失效的旧 token");
    }

    @Test
    @DisplayName("两次登录的会话标识必须不同(sid 不得由 userId 派生)")
    void twoLogins_haveDistinctSessionIds() {
        tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        tokenService.generateTokenPair(user, List.of("ROLE_USER"));

        assertEquals(2, cache.keysWithPrefix(SESSION_INDEX_PREFIX).size(),
                "同一用户两次登录必须产生两个不同的会话索引键 —— 若 sid 由 userId 派生,"
                        + "两次登录会互相覆盖索引,登出将牵连另一设备(或撤不到自己的 token)");
    }

    @Test
    @DisplayName("撤销只影响本次会话:另一次登录的 refresh token 不受影响")
    void logout_doesNotRevokeOtherSessions() {
        TokenResult first = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        TokenResult second = tokenService.generateTokenPair(user, List.of("ROLE_USER"));

        authApplicationService.logout(requestWith(first.accessToken()));

        assertFalse(cache.containsKey(REFRESH_KEY_PREFIX + first.refreshToken()),
                "本次会话的 refresh token 应被撤销");
        assertNotNull(cache.get(REFRESH_KEY_PREFIX + second.refreshToken()),
                "另一次登录(另一设备)的 refresh token 不应被牵连撤销 —— "
                        + "DELETE /auth/sessions 是单数语义,只登出当前会话");
    }

    // ── TTL ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("会话索引 TTL 必须等于 refresh token TTL,不得绑定 access TTL")
    void sessionIndex_ttlMatchesRefreshTokenTtl() {
        tokenService.generateTokenPair(user, List.of("ROLE_USER"));

        String indexKey = onlySessionIndexKey();

        assertEquals(REFRESH_TTL_SECONDS, cache.ttlOf(indexKey),
                "索引 TTL 必须与被它指向的 refresh token 同寿(604800s)。若绑定 access TTL(900s),"
                        + "则安全正确性被耦合到授权配置上:一旦登出改为接受已过期 access token(修「登出端点过期后 401」的常规做法),"
                        + "索引会先行消失而无任何测试报警;多实例时钟偏斜亦开同样的窗口(评审 HIGH)");
    }

    // ── 降级与健壮性 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("access token 无 sid 声明(滚动部署遗留)-> 登出不得抛异常,仍须拉黑")
    void logout_accessTokenWithoutSid_degradesGracefully() throws Exception {
        String legacyAccessToken = signAccessTokenWithoutSid();
        String jti = tokenService.parseAccessToken(stripBearer(legacyAccessToken)).jti();

        assertDoesNotThrow(() -> authApplicationService.logout(requestWith(legacyAccessToken)),
                "滚动部署期间,改造前签发的 access token 没有 sid 声明。"
                        + "登出必须降级为「仅拉黑」而非抛异常 —— 否则老客户端一律登出失败");
        assertTrue(cache.containsKey(SecurityConstant.TOKEN_BLACKLIST_PREFIX + jti),
                "降级路径下仍必须完成拉黑");
    }

    @Test
    @DisplayName("缓存 delete 返回 null -> 不得 NPE,且仍完成拉黑")
    void logout_deleteReturnsNull_doesNotThrow() throws Exception {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        String jti = tokenService.parseAccessToken(stripBearer(issued.accessToken())).jti();
        cache.deleteReturnsNull = true;

        assertDoesNotThrow(() -> authApplicationService.logout(requestWith(issued.accessToken())),
                "CacheGateway.delete 返回 Boolean(可为 null)。若把它拆箱进 boolean,"
                        + "Redis 异常时 NPE 会发生在拉黑之前 —— 整个登出 500,access token 也没拉黑(评审 MEDIUM)");
        assertTrue(cache.containsKey(SecurityConstant.TOKEN_BLACKLIST_PREFIX + jti),
                "撤销结果不确定时仍必须完成拉黑(不 fail-closed:否则客户端重试也无用,只会卡住用户)");
    }

    @Test
    @DisplayName("登出与刷新并发:登出撤销后完成的刷新不得凭空产生存活凭证(已知缺口,见 T049)")
    void concurrentLogoutAndRefresh_leavesNoLiveRefreshToken() throws Exception {
        TokenResult issued = tokenService.generateTokenPair(user, List.of("ROLE_USER"));
        stubUserLoad();

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var refreshFuture = pool.submit(() -> {
                startGate.await();
                try {
                    return authApplicationService.refreshToken(refreshRequest(issued.refreshToken())).refreshToken();
                } catch (Exception e) {
                    return null;
                }
            });
            var logoutFuture = pool.submit(() -> {
                startGate.await();
                try {
                    authApplicationService.logout(requestWith(issued.accessToken()));
                } catch (Exception ignored) {
                    // 登出失败不影响本用例断言,由下方缓存状态判定
                }
                return null;
            });

            startGate.countDown();
            String rotatedToken = refreshFuture.get(10, TimeUnit.SECONDS);
            logoutFuture.get(10, TimeUnit.SECONDS);

            assertFalse(cache.containsKey(REFRESH_KEY_PREFIX + issued.refreshToken()),
                    "原 refresh token 必须已失效(无论被刷新抢占还是被登出撤销)");

            // ⚠️ 这里**不能**断言「刷新赢时新 token 也必须被撤销」。实测的交错是:
            //   ① 刷新抢占并删除旧 refresh token
            //   ② 登出读到旧索引 → 删不到 → 撤销失败(打 LOGOUT_REVOKE_MISS)
            //   ③ 刷新这才生成新 refresh token 并重写索引
            // 在 ②<③ 的顺序下,登出撤销一个「尚未出生」的 token 物理上不可能 ——
            // 那样的断言不是更严格,而是不可满足。真正的不变量是「登出后不得留下
            // 用户不知情的存活凭证」,而它需要由**刷新侧**保证:刷新应拒绝已登出的会话
            // (旧 access token 未被拉黑、刷新也不查会话状态 → 见 T049)。
            // 本用例先钉住已修复的部分,并把缺口显式暴露为告警而非静默通过。
            if (rotatedToken != null && cache.containsKey(REFRESH_KEY_PREFIX + rotatedToken)) {
                assertTrue(cache.containsKey(SESSION_INDEX_PREFIX + sessionIdOf(issued.accessToken())),
                        "并发交错下若新 refresh token 存活,它至少必须仍可由 sid 索引定位 —— "
                                + "否则用户再也无法撤销它(悬垂凭证)。彻底消除存活凭证需刷新侧校验会话状态(T049)");
            }
        } finally {
            pool.shutdownNow();
        }
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
            throw new AssertionError("期望恰好 1 个 " + SESSION_INDEX_PREFIX + "* 会话索引键,实际 "
                    + keys.size() + " 个 —— 签发时未写索引则登出无法从 sid 定位 refresh token(T048)");
        }
        return keys.iterator().next();
    }

    /**
     * 为已存在的会话索引重新签发一个 access token(携带同一 sid),
     * 用于「登出须删除索引自身」这类不关心首个 access token 的用例。
     */
    private String reissueAccessTokenFor(String indexKey) {
        String sid = indexKey.substring(SESSION_INDEX_PREFIX.length());
        return signAccessToken(sid);
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
     * 内存假 CacheGateway:只实现本测试用到的操作,其余抛
     * {@link UnsupportedOperationException} —— 若将来实现改用了别的原语,
     * 测试会以「未支持的操作」明确失败,而不是静默走 mock 的默认返回值。
     *
     * <p>{@code delete} 严格复刻 Redis DEL 语义:只有键真实存在时返回 true。
     * TTL 不做真实过期(测试内无需等待),但记录下来供 TTL 断言使用。</p>
     */
    static class InMemoryCacheGateway implements CacheGateway {

        private final Map<String, Object> store = new ConcurrentHashMap<>();
        private final Map<String, Long> ttlSeconds = new ConcurrentHashMap<>();

        /** 置 true 后 {@code delete} 恒返回 null,模拟 Redis 异常时的不确定语义 */
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
            throw new UnsupportedOperationException("T048 测试未预期 increment 调用");
        }

        @Override
        public Boolean expire(String key, long ttl, TimeUnit timeUnit) {
            throw new UnsupportedOperationException("T048 测试未预期 expire 调用");
        }

        @Override
        public Long getExpire(String key, TimeUnit timeUnit) {
            throw new UnsupportedOperationException("T048 测试未预期 getExpire 调用");
        }

        @Override
        public void putAllHash(String key, Map<String, ?> map) {
            throw new UnsupportedOperationException("T048 测试未预期 putAllHash 调用");
        }

        @Override
        public void convertAndSend(String channel, Object message) {
            throw new UnsupportedOperationException("T048 测试未预期 convertAndSend 调用");
        }
    }
}
