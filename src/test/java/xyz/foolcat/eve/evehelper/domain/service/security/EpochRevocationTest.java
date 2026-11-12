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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * 改密纪元强制校验测试——refresh 路径(022 T006)。
 *
 * <p>测试矩阵(4 例,与 {@code JwtAuthorizationTokenFilterEpochTest} 同名对称):</p>
 * <ol>
 *   <li>{@code noMarker_legacyTokenWithoutIat_passes} — 无纪元标记 + 无 refresh_iat → 放行(部署兼容)</li>
 *   <li>{@code markerPresent_iatMissing_rejected} — 有纪元标记 + 无 refresh_iat → 拒绝(fail-closed)</li>
 *   <li>{@code markerPresent_iatBeforeEpoch_rejected} — iat ≤ epoch → 拒绝</li>
 *   <li>{@code markerPresent_iatAfterEpoch_passes} — iat > epoch → 放行</li>
 * </ol>
 *
 * <p>纯单元测试,不依赖 Spring 容器。</p>
 *
 * @author Leojan
 * date 2026-11-12
 */
@DisplayName("改密纪元强制校验 - refresh 路径(022 T006)")
class EpochRevocationTest {

    private static final String REFRESH_KEY_PREFIX = "refresh_token:";
    private static final String REFRESH_OWNER_PREFIX = "refresh_owner:";
    private static final String REFRESH_IAT_PREFIX = "refresh_iat:";
    private static final String REVOKE_EPOCH_PREFIX = "auth:user:revoke_epoch:";

    private static final String REFRESH_TOKEN = "aaaa1111-bbbb-2222-cccc-333344445555";
    private static final String SESSION_ID = "sess-epoch-test-001";
    private static final Integer USER_ID = 100;

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
        u.setUsername("epoch-tester");
        return u;
    }

    /**
     * 模拟 refresh token 缓存存在、归属存在、claim 成功、会话未登出。
     * refresh_iat 与 revoke_epoch 由用例单独配置。
     */
    private void stubRefreshTokenHappyPath() {
        String key = REFRESH_KEY_PREFIX + REFRESH_TOKEN;
        when(cacheGateway.get(eq(key))).thenReturn(USER_ID);
        when(cacheGateway.delete(eq(key))).thenReturn(true);

        String ownerKey = REFRESH_OWNER_PREFIX + REFRESH_TOKEN;
        when(cacheGateway.get(eq(ownerKey))).thenReturn(SESSION_ID);
    }

    // ────────────────────────────────────────────────────────────────
    //  测试矩阵
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("noMarker_legacyTokenWithoutIat_passes — 无纪元标记 + 无 refresh_iat → 放行(部署兼容)")
    void noMarker_legacyTokenWithoutIat_passes() {
        stubRefreshTokenHappyPath();
        // refresh_iat 不存在(legacy 会话,无 T005 写入)
        when(cacheGateway.get(eq(REFRESH_IAT_PREFIX + SESSION_ID))).thenReturn(null);
        // 无纪元标记(用户从未改密)
        when(cacheGateway.get(eq(REVOKE_EPOCH_PREFIX + USER_ID))).thenReturn(null);

        TokenResult result = tokenService.refreshAccessTokenWithUser(
                REFRESH_TOKEN, user(), List.of("ROLE_USER"));

        assertNotNull(result, "无纪元标记时应正常签发新 token 对(部署兼容)");
        assertNotNull(result.accessToken(), "应签发新 accessToken");
        assertNotNull(result.refreshToken(), "应签发新 refreshToken");
    }

    @Test
    @DisplayName("markerPresent_iatMissing_rejected — 有纪元标记 + 无 refresh_iat → 拒绝(fail-closed)")
    void markerPresent_iatMissing_rejected() {
        stubRefreshTokenHappyPath();
        // refresh_iat 不存在(legacy 会话)
        when(cacheGateway.get(eq(REFRESH_IAT_PREFIX + SESSION_ID))).thenReturn(null);
        // 有纪元标记(用户已改密)
        when(cacheGateway.get(eq(REVOKE_EPOCH_PREFIX + USER_ID))).thenReturn(System.currentTimeMillis());

        assertThrows(IllegalArgumentException.class, () ->
                tokenService.refreshAccessTokenWithUser(
                        REFRESH_TOKEN, user(), List.of("ROLE_USER")),
                "有纪元标记但无 iat 时必须拒绝(fail-closed:无法判定会话是否在改密后创建)");
    }

    @Test
    @DisplayName("markerPresent_iatBeforeEpoch_rejected — iat ≤ epoch → 拒绝")
    void markerPresent_iatBeforeEpoch_rejected() {
        stubRefreshTokenHappyPath();
        long epoch = 1_700_000_000_000L;
        long iat = epoch - 5000L; // iat 早于 epoch 5 秒
        when(cacheGateway.get(eq(REFRESH_IAT_PREFIX + SESSION_ID))).thenReturn(iat);
        when(cacheGateway.get(eq(REVOKE_EPOCH_PREFIX + USER_ID))).thenReturn(epoch);

        assertThrows(IllegalArgumentException.class, () ->
                tokenService.refreshAccessTokenWithUser(
                        REFRESH_TOKEN, user(), List.of("ROLE_USER")),
                "iat ≤ epoch 表示会话在改密之前创建,必须拒绝");
    }

    @Test
    @DisplayName("markerPresent_iatAfterEpoch_passes — iat > epoch → 放行")
    void markerPresent_iatAfterEpoch_passes() {
        stubRefreshTokenHappyPath();
        long epoch = 1_700_000_000_000L;
        long iat = epoch + 2000L; // iat 晚于 epoch 2 秒(≥2 秒裕量,避免秒截断翻转)
        when(cacheGateway.get(eq(REFRESH_IAT_PREFIX + SESSION_ID))).thenReturn(iat);
        when(cacheGateway.get(eq(REVOKE_EPOCH_PREFIX + USER_ID))).thenReturn(epoch);

        TokenResult result = tokenService.refreshAccessTokenWithUser(
                REFRESH_TOKEN, user(), List.of("ROLE_USER"));

        assertNotNull(result, "iat > epoch 表示会话在改密之后创建,应正常放行");
        assertNotNull(result.accessToken(), "应签发新 accessToken");
    }
}
