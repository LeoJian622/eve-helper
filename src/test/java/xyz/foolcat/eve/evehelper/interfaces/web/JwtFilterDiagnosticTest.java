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

import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;

/**
 * JWT 过滤器行为诊断测试(007 设计评审 C2/C3 实测)。
 *
 * <p><b>这不是交付测试,而是诊断测试。</b>目的是实测安全评审提出的两个断言,
 * 决定 007 规格是否需要修订:</p>
 * <ul>
 *   <li><b>C3</b>:验签失败时实际返回什么状态码?评审称因 JWT 过滤器位于
 *       {@code ExceptionTranslationFilter} 上游,抛出的 {@code InvalidCookieException}
 *       会冒泡到容器产生 500,而非 401 —— 若成立则 007 的 US1/SC-002 前提被推翻</li>
 *   <li><b>C2</b>:{@code POST /auth/tokens} 在无 token / 失效 token 两种情形下是否可达?
 *       该端点不在 whiteUrlList(全部 profile 仅 {@code POST:/user}),若不可达则
 *       「不强制登出」的无感方案跑不通</li>
 * </ul>
 *
 * <p>测试用的 token 由<b>测试内现场生成的</b> RSA 密钥签名,与生产 keystore 公钥必然不匹配,
 * 故一定验签失败。不涉及任何真实凭证。</p>
 *
 * <p>本测试只观测与记录,不断言"应该"是什么 —— 断言写在修复之后。</p>
 *
 * @author Leojan
 * date 2026-08-11
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("JWT 过滤器行为诊断(007 C2/C3)")
class JwtFilterDiagnosticTest {

    @Autowired
    MockMvc mockMvc;

    /**
     * 构造一个签名有效但公钥不匹配的 JWT ——
     * 模拟"密钥轮换后,用旧密钥签发的 token"这一确切场景。
     */
    private static String tokenSignedByForeignKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        java.security.KeyPair foreign = generator.generateKeyPair();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("eve-helper")
                .issuer("eve-helper.xyz")
                .jwtID("diagnostic-jti-0001")
                .claim("userId", 1)
                .claim("username", "diagnostic")
                .claim("authorities", List.of("ROOT"))
                .expirationTime(new Date(System.currentTimeMillis() + 600_000))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claims);
        jwt.sign(new RSASSASigner(foreign.getPrivate()));
        return jwt.serialize();
    }

    @Test
    @DisplayName("C3: 验签失败(旧密钥签发的 token)→ 实际状态码是什么")
    void diagnose_signatureVerificationFailure_actualStatus() throws Exception {
        String foreignToken = tokenSignedByForeignKey();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/structures/98000001")
                        .header("Authorization", "Bearer " + foreignToken))
                .andReturn();

        Exception resolved = result.getResolvedException();
        System.out.println("=== [C3] 验签失败场景 ===");
        System.out.println("[C3] HTTP status      = " + result.getResponse().getStatus());
        System.out.println("[C3] body             = " + result.getResponse().getContentAsString());
        System.out.println("[C3] resolvedException= " + (resolved == null ? "null" : resolved.getClass().getName()));
        System.out.println("[C3] 说明: 若 status 非 401,则 007 规格 6.1「必然是 401」的论断被推翻");
    }

    @Test
    @DisplayName("C2: POST /auth/tokens 无 Authorization 头 → 是否可达")
    void diagnose_refreshEndpoint_withoutToken() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/auth/tokens")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andReturn();

        System.out.println("=== [C2a] refresh 端点 / 无 token ===");
        System.out.println("[C2a] HTTP status = " + result.getResponse().getStatus());
        System.out.println("[C2a] body        = " + result.getResponse().getContentAsString());
        System.out.println("[C2a] 说明: 403/401 => 端点不可达,「无感」方案跑不通;"
                + "400/其它业务错误 => 已到达 controller,方案可行");
    }

    @Test
    @DisplayName("C2: POST /auth/tokens 带失效 token → 是否可达")
    void diagnose_refreshEndpoint_withInvalidToken() throws Exception {
        String foreignToken = tokenSignedByForeignKey();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/auth/tokens")
                        .header("Authorization", "Bearer " + foreignToken)
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andReturn();

        System.out.println("=== [C2b] refresh 端点 / 带失效 token ===");
        System.out.println("[C2b] HTTP status = " + result.getResponse().getStatus());
        System.out.println("[C2b] body        = " + result.getResponse().getContentAsString());
        System.out.println("[C2b] 说明: 若过滤器在验签失败时中断请求,则带旧 token 调 refresh 也换不到新 token");
    }
}
