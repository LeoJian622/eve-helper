package xyz.foolcat.eve.evehelper.interfaces.web;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import xyz.foolcat.eve.evehelper.domain.service.security.TokenBlacklistService;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 认证失败 401 链路测试(007 FR-016,T008;由 {@code JwtFilterDiagnosticTest} 改写)。
 *
 * <p>诊断测试(C2/C3,已归档于三轮评审记录)证实:验签/过期/黑名单失败时,
 * {@code JwtAuthorizationTokenFilter} 抛 {@code InvalidCookieException} 冒泡为异常逃逸,
 * 而非干净的 401。本测试固化修复后的目标行为(T013 实现的 RED→GREEN 判据):</p>
 * <ul>
 *   <li>验签失败(外部密钥签发的 token,模拟密钥轮换后旧 token)→ <b>401</b> + body code {@code AUT00210}</li>
 *   <li>过期 token(应用密钥签发但已过期)→ 401 + {@code AUT00210}</li>
 *   <li>黑名单 token(应用密钥签发、未过期、jti 已撤销)→ 401 + {@code AUT00210}</li>
 *   <li>响应体<b>不含 token 片段</b>(防敏感数据回显)</li>
 *   <li>带旧 token 请求<b>非白名单</b>端点 → 401(spec AC#3,不得因白名单改造放宽)</li>
 * </ul>
 *
 * <p>统一返回 {@code AUT00210} 是防信息泄露的有意决策(LOW-2):不向客户端区分
 * 「验签失败/过期/被撤销」,避免泄露服务端密钥状态。</p>
 *
 * <p>测试 token 均为测试内现场生成:外部密钥与应用测试密钥(test-only.jks)签名,
 * 不涉及任何真实凭证。</p>
 *
 * <p>TDD 状态:T008 = RED(现状为 InvalidCookieException 异常逃逸,MockMvc 抛出而非返回 401);
 * T013 filter 重写后转 GREEN。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("JWT 认证失败 401 链路(007 FR-016)")
class JwtAuthFailureResponseTest {

    /** 非白名单受保护端点(whiteUrlList 现仅 POST:/user,007 后将增 POST:/auth/tokens) */
    private static final String PROTECTED_ENDPOINT = "/structures/98000001";

    @Autowired
    MockMvc mockMvc;

    /** 应用测试密钥对(test-only.jks)—— 用于签发「能通过验签」的 token */
    @Autowired
    KeyPair keyPair;

    @Autowired
    TokenBlacklistService tokenBlacklistService;

    /**
     * 外部密钥签发的 token —— 模拟「密钥轮换后,用旧密钥签发的 token」这一确切场景。
     * 与应用公钥必然不匹配,一定验签失败。
     */
    private static String tokenSignedByForeignKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        java.security.KeyPair foreign = generator.generateKeyPair();
        return sign(foreign.getPrivate(), "foreign-jti-0001",
                new Date(System.currentTimeMillis() + 600_000));
    }

    /** 应用测试密钥签发的 token,可控过期时间与 jti */
    private String tokenSignedByAppKey(String jti, Date expiration) throws Exception {
        return sign(keyPair.getPrivate(), jti, expiration);
    }

    private static String sign(java.security.PrivateKey privateKey, String jti, Date expiration) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("eve-helper")
                .issuer("eve-helper.xyz")
                .jwtID(jti)
                .claim("userId", 1)
                .claim("username", "t008-test")
                .claim("authorities", List.of("ROOT"))
                .expirationTime(expiration)
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claims);
        jwt.sign(new RSASSASigner(privateKey));
        return jwt.serialize();
    }

    @Test
    @DisplayName("验签失败(旧密钥 token)请求非白名单端点 → 401 + AUT00210(AC#3)")
    void verifyFailure_nonWhitelistedEndpoint_returns401WithAut00210() throws Exception {
        // Arrange
        String foreignToken = tokenSignedByForeignKey();

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + foreignToken))
                .andReturn();

        // Assert
        String body = result.getResponse().getContentAsString();
        assertEquals(401, result.getResponse().getStatus(),
                "验签失败应为干净的 401(现状为异常逃逸);body=" + body);
        assertTrue(body.contains("AUT00210"), "响应体应含统一错误码 AUT00210,实际: " + body);
        assertFalse(body.contains(foreignToken.substring(0, 32)),
                "响应体不得回显 token 片段");
    }

    @Test
    @DisplayName("过期 token(应用密钥签发)→ 401 + AUT00210")
    void expiredToken_returns401WithAut00210() throws Exception {
        // Arrange:应用密钥签名(能通过验签)但已过期
        String expiredToken = tokenSignedByAppKey("t008-expired-jti",
                new Date(System.currentTimeMillis() - 600_000));

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + expiredToken))
                .andReturn();

        // Assert
        String body = result.getResponse().getContentAsString();
        assertEquals(401, result.getResponse().getStatus(),
                "过期 token 应为干净的 401;body=" + body);
        assertTrue(body.contains("AUT00210"), "响应体应含 AUT00210,实际: " + body);
        assertFalse(body.contains(expiredToken.substring(0, 32)),
                "响应体不得回显 token 片段");
    }

    @Test
    @DisplayName("黑名单 token(已撤销 jti)→ 401 + AUT00210")
    void blacklistedToken_returns401WithAut00210() throws Exception {
        // Arrange:应用密钥签名、未过期,但 jti 已加入黑名单
        String jti = "t008-blacklisted-jti";
        Date expiration = new Date(System.currentTimeMillis() + 600_000);
        String blacklistedToken = tokenSignedByAppKey(jti, expiration);
        tokenBlacklistService.addToBlacklist(jti, expiration);

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + blacklistedToken))
                .andReturn();

        // Assert:撤销与验签失败/过期统一返回 AUT00210(不区分,防信息泄露)
        String body = result.getResponse().getContentAsString();
        assertEquals(401, result.getResponse().getStatus(),
                "黑名单 token 应为干净的 401;body=" + body);
        assertTrue(body.contains("AUT00210"), "响应体应含 AUT00210,实际: " + body);
    }
}
