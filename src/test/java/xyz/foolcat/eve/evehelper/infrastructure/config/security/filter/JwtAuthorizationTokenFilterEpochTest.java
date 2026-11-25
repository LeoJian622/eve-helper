package xyz.foolcat.eve.evehelper.infrastructure.config.security.filter;

import cn.hutool.json.JSONUtil;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenBlacklistService;
import xyz.foolcat.eve.evehelper.infrastructure.config.security.WhiteUrlMatcher;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.SecurityConstant;
import xyz.foolcat.eve.evehelper.shared.result.Result;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyPair;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 改密纪元强制校验测试——filter 路径(022 T006)。
 *
 * <p>测试矩阵(4 例,与 {@code EpochRevocationTest} 同名对称):</p>
 * <ol>
 *   <li>{@code noMarker_legacyTokenWithoutIat_passes} — 无纪元标记 + 无 iat → 放行(部署兼容)</li>
 *   <li>{@code markerPresent_iatMissing_rejected} — 有纪元标记 + 无 iat → 401</li>
 *   <li>{@code markerPresent_iatBeforeEpoch_rejected} — iat ≤ epoch → 401</li>
 *   <li>{@code markerPresent_iatAfterEpoch_passes} — iat > epoch → 放行</li>
 * </ol>
 *
 * <p>纯单元测试:Mockito mock servlet 对象 + Nimbus 签发真实 JWT。不使用 Spring 容器。</p>
 *
 * @author Leojan
 * date 2026-11-12
 */
@DisplayName("改密纪元强制校验 - filter 路径(022 T006)")
class JwtAuthorizationTokenFilterEpochTest {

    private static final String REVOKE_EPOCH_PREFIX = "auth:user:revoke_epoch:";
    private static final Integer USER_ID = 100;
    private static final String SESSION_ID = "sess-filter-epoch-001";

    private KeyPair keyPair;
    private CacheGateway cacheGateway;
    private TokenBlacklistService tokenBlacklistService;
    private WhiteUrlMatcher whiteUrlMatcher;
    private JwtAuthorizationTokenFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private ByteArrayOutputStream responseOutput;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = new RSAKeyGenerator(2048).generate().toKeyPair();
        cacheGateway = mock(CacheGateway.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        whiteUrlMatcher = mock(WhiteUrlMatcher.class);

        filter = new JwtAuthorizationTokenFilter(keyPair, tokenBlacklistService, whiteUrlMatcher, cacheGateway);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        responseOutput = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(jakarta.servlet.WriteListener listener) { }
            @Override
            public void write(int b) { responseOutput.write(b); }
            @Override
            public void write(byte[] b, int off, int len) { responseOutput.write(b, off, len); }
        });
        when(response.isCommitted()).thenReturn(false);
        when(whiteUrlMatcher.isWhiteListed(any())).thenReturn(false);
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
    }

    /**
     * 签发测试用 JWT。
     *
     * @param includeIat 是否包含 iat 声明;false 时不设置 issueTime(旧 token 兼容)
     * @param iatMillis  iat 毫秒时间戳(仅 includeIat=true 时生效)
     */
    private String signJwt(boolean includeIat, long iatMillis) throws Exception {
        long now = System.currentTimeMillis();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject("eve-helper-test")
                .issuer("eve-helper-test")
                .jwtID("jti-epoch-" + now)
                .claim(SecurityConstant.USER_ID_KEY, USER_ID)
                .claim(SecurityConstant.USER_NAME_KEY, "epoch-tester")
                .claim(SecurityConstant.JWT_AUTHORITIES_KEY, List.of("ROLE_USER"))
                .claim(SecurityConstant.SESSION_ID_KEY, SESSION_ID)
                .expirationTime(new Date(now + 900_000L));

        if (includeIat) {
            builder.issueTime(new Date(iatMillis));
        }
        // 不设置 issueTime → getIssueTime() 返回 null → iat == null

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                builder.build());
        jwt.sign(new RSASSASigner(keyPair.getPrivate()));
        return jwt.serialize();
    }

    private void setTokenHeader(String token) {
        when(request.getHeader(SecurityConstant.AUTHORIZATION_KEY))
                .thenReturn(SecurityConstant.JWT_PREFIX + token);
    }

    /** 断言响应写出 401 + AUT00210 */
    private void assertRejected() throws Exception {
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
        String body = responseOutput.toString();
        assertTrue(body.contains("AUT00210"),
                "响应体应包含 AUT00210,实际: " + body);
    }

    /** 断言过滤器链放行 */
    private void assertPassed() throws Exception {
        verify(filterChain).doFilter(request, response);
    }

    // ────────────────────────────────────────────────────────────────
    //  测试矩阵
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("noMarker_legacyTokenWithoutIat_passes — 无纪元标记 + 无 iat → 放行(部署兼容)")
    void noMarker_legacyTokenWithoutIat_passes() throws Exception {
        String token = signJwt(false, 0L);
        setTokenHeader(token);
        // 无纪元标记(用户从未改密)
        when(cacheGateway.get(eq(REVOKE_EPOCH_PREFIX + USER_ID))).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertPassed();
    }

    @Test
    @DisplayName("markerPresent_iatMissing_rejected — 有纪元标记 + 无 iat → 401")
    void markerPresent_iatMissing_rejected() throws Exception {
        String token = signJwt(false, 0L);
        setTokenHeader(token);
        // 有纪元标记(用户已改密)
        when(cacheGateway.get(eq(REVOKE_EPOCH_PREFIX + USER_ID)))
                .thenReturn(System.currentTimeMillis());

        filter.doFilterInternal(request, response, filterChain);

        assertRejected();
    }

    @Test
    @DisplayName("markerPresent_iatBeforeEpoch_rejected — iat ≤ epoch → 401")
    void markerPresent_iatBeforeEpoch_rejected() throws Exception {
        long epoch = 1_700_000_000_000L;
        long iat = epoch - 5000L;
        String token = signJwt(true, iat);
        setTokenHeader(token);
        when(cacheGateway.get(eq(REVOKE_EPOCH_PREFIX + USER_ID))).thenReturn(epoch);

        filter.doFilterInternal(request, response, filterChain);

        assertRejected();
    }

    @Test
    @DisplayName("markerPresent_iatAfterEpoch_passes — iat > epoch → 放行")
    void markerPresent_iatAfterEpoch_passes() throws Exception {
        long epoch = 1_700_000_000_000L;
        long iat = epoch + 2000L; // ≥2 秒裕量,避免秒截断翻转
        String token = signJwt(true, iat);
        setTokenHeader(token);
        when(cacheGateway.get(eq(REVOKE_EPOCH_PREFIX + USER_ID))).thenReturn(epoch);

        filter.doFilterInternal(request, response, filterChain);

        assertPassed();
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
