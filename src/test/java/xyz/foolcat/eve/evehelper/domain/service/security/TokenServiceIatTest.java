package xyz.foolcat.eve.evehelper.domain.service.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.model.vo.TokenResult;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.config.JwtTokenProperties;

import java.security.KeyPair;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TokenService iat claim 与会话索引测试(022 T005)。
 *
 * <p>验证四项契约:access token 含 iat claim;生成时写入 refresh_iat 与 refresh_user 索引;
 * 解析结果暴露 iat;旧 token(无 iat)解析返回 null。</p>
 *
 * @author Leojan
 * date 2026-11-12
 */
@DisplayName("TokenService iat claim + 会话索引(022 T005)")
class TokenServiceIatTest {

    private static final Integer USER_ID = 42;
    private static final String REFRESH_IAT_PREFIX = "refresh_iat:";
    private static final String REFRESH_USER_PREFIX = "refresh_user:";

    private CacheGateway cacheGateway;
    private TokenService tokenService;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        cacheGateway = Mockito.mock(CacheGateway.class);
        JwtTokenProperties props = new JwtTokenProperties();
        props.setAccessTokenExpirationTime(900L);
        props.setRefreshTokenExpirationTime(604800L);
        props.setIssuer("eve-helper-test");
        props.setSubject("eve-helper-test");

        keyPair = new RSAKeyGenerator(2048).generate().toKeyPair();
        tokenService = new TokenService(keyPair, props, cacheGateway);

        doNothing().when(cacheGateway).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        when(cacheGateway.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(cacheGateway.sAdd(anyString(), any())).thenReturn(1L);
        when(cacheGateway.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    private static SysUser user() {
        SysUser u = new SysUser();
        u.setId(USER_ID);
        u.setUsername("tester");
        return u;
    }

    @Test
    @DisplayName("generateTokenPair: access token 含 iat claim")
    void generateTokenPair_accessTokenContainsIat() throws ParseException {
        long beforeMs = System.currentTimeMillis();
        TokenResult result = tokenService.generateTokenPair(user(), List.of("ROLE_USER"));
        long afterMs = System.currentTimeMillis();

        assertNotNull(result.accessToken(), "accessToken 不得为 null");
        String raw = result.accessToken().replace("Bearer ", "");
        SignedJWT jwt = SignedJWT.parse(raw);
        Date iat = jwt.getJWTClaimsSet().getIssueTime();
        assertNotNull(iat, "access token 必须包含 iat claim");
        // JWT iat 以秒精度存储(NumericDate),毫秒被截断
        long iatSeconds = iat.getTime() / 1000;
        long beforeSeconds = beforeMs / 1000;
        long afterSeconds = afterMs / 1000;
        assertTrue(iatSeconds >= beforeSeconds && iatSeconds <= afterSeconds,
                "iat 应在签发时间窗口内(秒精度)");
    }

    @Test
    @DisplayName("generateTokenPair: 写入 refresh_iat 与 refresh_user 索引")
    void generateTokenPair_writesRefreshIatAndUserIndex() {
        long beforeMs = System.currentTimeMillis();
        tokenService.generateTokenPair(user(), List.of("ROLE_USER"));
        long afterMs = System.currentTimeMillis();

        // refresh_iat:{sid} 用 setIfAbsent 写入,TTL = refreshTtl
        verify(cacheGateway).setIfAbsent(
                Mockito.startsWith(REFRESH_IAT_PREFIX),
                any(),
                Mockito.eq(604800L),
                Mockito.eq(TimeUnit.SECONDS));

        // refresh_user:{userId} 用 sAdd 写入,随后 expire 续期
        verify(cacheGateway).sAdd(Mockito.eq(REFRESH_USER_PREFIX + USER_ID), anyString());
        verify(cacheGateway).expire(Mockito.eq(REFRESH_USER_PREFIX + USER_ID),
                Mockito.eq(604800L), Mockito.eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("parseAccessToken: 解析结果暴露 iat(毫秒)")
    void parseAccessToken_exposesIat() throws Exception {
        TokenResult result = tokenService.generateTokenPair(user(), List.of("ROLE_USER"));
        String raw = result.accessToken().replace("Bearer ", "");

        TokenService.ParsedAccessToken parsed = tokenService.parseAccessToken(raw);
        assertNotNull(parsed.iat(), "iat 不得为 null");
        assertTrue(parsed.iat() > 0, "iat 应为正数毫秒");
    }

    @Test
    @DisplayName("parseAccessToken: 旧 token 无 iat → 返回 null iat")
    void parseAccessToken_legacyTokenWithoutIat_returnsNullIat() throws Exception {
        // 手动签发不含 iat 的 access token(模拟改造前签发的旧 token)
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("eve-helper-test")
                .issuer("eve-helper-test")
                .jwtID("legacy-jti")
                .claim("userId", USER_ID)
                .claim("username", "tester")
                .claim("authorities", List.of("ROLE_USER"))
                .claim("sid", "legacy-sid")
                .expirationTime(new Date(System.currentTimeMillis() + 900_000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claimsSet);
        signedJWT.sign(new RSASSASigner(keyPair.getPrivate()));
        String legacyToken = signedJWT.serialize();

        TokenService.ParsedAccessToken parsed = tokenService.parseAccessToken(legacyToken);
        assertNull(parsed.iat(), "旧 token 无 iat claim 时应返回 null");
    }
}
