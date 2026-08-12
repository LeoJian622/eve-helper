package xyz.foolcat.eve.evehelper.domain.service.security;

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.TokenResult;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.config.JwtTokenProperties;

import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Refresh Token 轮换的并发安全契约测试(007 T042,评审 MEDIUM-3)。
 *
 * <p><b>残留 TOCTOU 窗口</b>:T015 只消除了一半 —— {@code AuthApplicationService.refreshToken}
 * 内合并了 {@code hasKey}+{@code get},但 {@code TokenService.refreshAccessTokenWithUser}
 * <b>又做了一次 {@code cacheGateway.get(key)}</b>,窗口从此平移到:</p>
 * <pre>
 *   ① getUserIdFromRefreshToken → get(key)      ← 校验通过
 *      ├─ loadUserById             (DB 往返 1)
 *      ├─ queryRolesByUserId       (DB 往返 2)
 *   ③ refreshAccessTokenWithUser  → get(key)    ← 再次校验通过
 *   ④ revokeRefreshToken          → delete(key) ← 此刻才失效
 * </pre>
 * <p>窗口 = ①~④,<b>跨越两次 DB 往返被显著拉长</b>。同一 refresh token 的并发双请求
 * 都能在 ④ 之前通过 ③ 的校验,于是<b>各得一套有效 token 对</b> —— 轮换机制的
 * 「一次性」保证失效,被盗 token 与合法用户可并存。</p>
 *
 * <p><b>修复取向</b>:不需要 Lua 或 GETDEL —— Redis {@code DEL} 本身即原子,
 * {@code redisTemplate.delete(key)} 返回<b>是否真的删掉了</b>。将「撤销」前移为
 * <b>compare-and-claim</b>:只有 {@code delete} 返回 true 的那个请求才有权继续,
 * 其余一律拒绝。现有实现丢弃了该返回值,这正是可利用性的根源。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@DisplayName("Refresh Token 轮换并发安全(007 T042 残留 TOCTOU)")
class TokenServiceConcurrentRefreshTest {

    private static final String REFRESH_KEY_PREFIX = "refresh_token:";
    private static final String TOKEN = "11111111-2222-3333-4444-555555555555";
    private static final Integer USER_ID = 42;

    private CacheGateway cacheGateway;
    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        cacheGateway = Mockito.mock(CacheGateway.class);
        JwtTokenProperties props = new JwtTokenProperties();
        props.setAccessTokenExpirationTime(900L);
        props.setRefreshTokenExpirationTime(604800L);
        props.setIssuer("eve-helper-test");
        props.setSubject("eve-helper-test");

        KeyPair keyPair = new RSAKeyGenerator(2048).generate().toKeyPair();
        tokenService = new TokenService(keyPair, props, cacheGateway);

        doNothing().when(cacheGateway).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    private static SysUser user() {
        SysUser u = new SysUser();
        u.setId(USER_ID);
        u.setUsername("tester");
        return u;
    }

    /**
     * 并发双请求持同一 refresh token:必须只有一个成功。
     *
     * <p>用 mock 精确模拟 Redis 语义:{@code get} 在被 {@code delete} 成功前恒返回
     * userId;{@code delete} 只有<b>第一次</b>返回 true(DEL 的真实行为)。</p>
     */
    @Test
    @DisplayName("同一 refresh token 并发刷新 → 只有一个请求可拿到新 token 对")
    void concurrentRefreshWithSameToken_onlyOneSucceeds() throws Exception {
        String key = REFRESH_KEY_PREFIX + TOKEN;
        AtomicInteger deleteCalls = new AtomicInteger();

        // Redis 语义:key 未被删除前 get 恒有值
        when(cacheGateway.get(eq(key))).thenAnswer(inv ->
                deleteCalls.get() == 0 ? USER_ID : null);
        // Redis DEL 语义:只有真正删掉的那次返回 true
        when(cacheGateway.delete(eq(key))).thenAnswer(inv ->
                deleteCalls.incrementAndGet() == 1);

        int threads = 2;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ConcurrentLinkedQueue<TokenResult> succeeded = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Exception> rejected = new ConcurrentLinkedQueue<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        startGate.await();
                        TokenResult result = tokenService.refreshAccessTokenWithUser(
                                TOKEN, user(), List.of("ROLE_USER"));
                        succeeded.add(result);
                    } catch (Exception e) {
                        rejected.add(e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            startGate.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS), "并发任务应在 10s 内结束");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, succeeded.size(),
                "同一 refresh token 并发刷新只能有 1 个成功,实际成功 " + succeeded.size()
                        + " 个 —— 轮换的「一次性」保证失效,被盗 token 可与合法用户并存(T042)");
        assertEquals(1, rejected.size(),
                "另一个请求必须被明确拒绝,实际被拒 " + rejected.size() + " 个");
    }

    /** 单请求正常路径不得被 claim 改造破坏 */
    @Test
    @DisplayName("单请求刷新:正常返回新 token 对")
    void singleRefresh_succeeds() {
        String key = REFRESH_KEY_PREFIX + TOKEN;
        when(cacheGateway.get(eq(key))).thenReturn(USER_ID);
        when(cacheGateway.delete(eq(key))).thenReturn(true);

        TokenResult result = tokenService.refreshAccessTokenWithUser(
                TOKEN, user(), List.of("ROLE_USER"));

        assertTrue(result.accessToken() != null && !result.accessToken().isBlank(),
                "应签发新 accessToken");
        assertTrue(result.refreshToken() != null && !result.refreshToken().isBlank(),
                "应签发新 refreshToken");
        assertTrue(!TOKEN.equals(result.refreshToken()),
                "新 refreshToken 必须与旧值不同(轮换)");
    }

    /**
     * claim 失败(delete 返回 false)必须拒绝,且<b>不得签发任何 token</b>。
     *
     * <p>这是 T042 的核心语义:delete 返回 false 意味着「别人已经用掉了这个 token」,
     * 无论此前的 get 校验是否通过。</p>
     */
    @Test
    @DisplayName("claim 失败(token 已被他人用掉)→ 拒绝且不签发 token")
    void claimLost_rejectsWithoutIssuingTokens() {
        String key = REFRESH_KEY_PREFIX + TOKEN;
        // get 仍返回值(模拟窗口内的陈旧读),但 delete 报「不是我删的」
        when(cacheGateway.get(eq(key))).thenReturn(USER_ID);
        when(cacheGateway.delete(eq(key))).thenReturn(false);

        assertTrue(throwsAnyException(() -> tokenService.refreshAccessTokenWithUser(
                        TOKEN, user(), List.of("ROLE_USER"))),
                "delete 返回 false 必须导致拒绝 —— 否则并发双方都能拿到 token");

        // 关键:拒绝路径不得写入任何新的 refresh token
        Mockito.verify(cacheGateway, Mockito.never())
                .set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    /** delete 返回 null(Redis 连接异常等)按失败处理 —— fail-closed */
    @Test
    @DisplayName("delete 返回 null → 按 claim 失败拒绝(fail-closed)")
    void claimReturnsNull_rejects() {
        String key = REFRESH_KEY_PREFIX + TOKEN;
        when(cacheGateway.get(eq(key))).thenReturn(USER_ID);
        when(cacheGateway.delete(eq(key))).thenReturn(null);

        assertTrue(throwsAnyException(() -> tokenService.refreshAccessTokenWithUser(
                        TOKEN, user(), List.of("ROLE_USER"))),
                "delete 返回 null 语义不明,必须 fail-closed 拒绝而非放行");
    }

    private static boolean throwsAnyException(Runnable action) {
        try {
            action.run();
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
