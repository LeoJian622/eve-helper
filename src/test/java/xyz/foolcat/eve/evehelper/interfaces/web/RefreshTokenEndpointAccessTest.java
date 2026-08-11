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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * refresh 端点可达性测试(007 FR-020,T009;spec US1 场景 2,AC#1/#2)。
 *
 * <p><b>问题背景</b>:密钥轮换后客户端持旧 token,必然要走
 * {@code POST /auth/tokens} 换新 token。若该端点要求「先有有效 access token」,
 * 客户端将陷入「401 → refresh → 401 → refresh」死循环。因此 refresh 端点必须
 * 在<b>无 token / 旧签名 token / 格式非法 token</b> 三种情形下都能到达 controller。</p>
 *
 * <p><b>「到达 controller」的判据</b>:断言响应为业务错误
 * {@code 400 + 「Refresh Token无效或已过期」} —— 该消息仅由
 * {@code AuthApplicationService.refreshToken}(:116)生成,出现即证明请求
 * 穿过了 filter 与 RBAC、进入了应用服务层,而非仅断言状态码。</p>
 *
 * <p><b>refresh token 不存在的场景</b>:测试用合成 UUID 在 Redis 中无对应记录,
 * 必走「无效或已过期」分支 —— 即 tasks 要求的「明确业务错误(400 + 无效或已过期)」。</p>
 *
 * <p>TDD 状态:T009 = RED —— 现状①过滤器对旧/非法 token 抛
 * {@code InvalidCookieException}(异常逃逸),②端点不在 whiteUrlList,
 * 无 token 时被 RBAC 拒绝。T013(filter 白名单放行)+ T014(加白
 * {@code POST:/auth/tokens})落地后转 GREEN。</p>
 *
 * <p>测试 token 为测试内现场生成的外部密钥签名,不涉及任何真实凭证。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("refresh 端点可达性(007 FR-020)")
class RefreshTokenEndpointAccessTest {

    private static final String REFRESH_ENDPOINT = "/auth/tokens";

    /** 合成 UUID:格式合法但 Redis 中不存在 → 业务错误「无效或已过期」 */
    private static final String NON_EXISTENT_REFRESH_TOKEN =
            "{\"refreshToken\":\"00000000-0000-0000-0000-000000000000\"}";

    /** 到达 controller 的判据:仅 AuthApplicationService:116 能产生此消息 */
    private static final String SERVICE_REACHED_EVIDENCE = "无效或已过期";

    @Autowired
    MockMvc mockMvc;

    private static String tokenSignedByForeignKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        java.security.KeyPair foreign = generator.generateKeyPair();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("eve-helper")
                .issuer("eve-helper.xyz")
                .jwtID("t009-foreign-jti")
                .claim("userId", 1)
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
    @DisplayName("无 token 请求 refresh 端点 → 到达 controller(业务错误,非 401/403)")
    void noToken_reachesController() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(REFRESH_ENDPOINT)
                        .contentType("application/json")
                        .content(NON_EXISTENT_REFRESH_TOKEN))
                .andReturn();

        // Assert:现状无白名单时被 RBAC 拒 403 → RED;T014 加白后放行至 controller
        String body = result.getResponse().getContentAsString();
        assertEquals(400, result.getResponse().getStatus(),
                "无 token 也应到达 controller 并返回业务错误(现状被 RBAC 拦截);body=" + body);
        assertTrue(body.contains(SERVICE_REACHED_EVIDENCE),
                "应到达 AuthApplicationService(消息「无效或已过期」是其唯一来源);实际 body=" + body);
    }

    @Test
    @DisplayName("带旧签名 token 请求 refresh 端点 → 到达 controller(AC#1)")
    void foreignSignedToken_reachesController() throws Exception {
        // Arrange
        String foreignToken = tokenSignedByForeignKey();

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(REFRESH_ENDPOINT)
                        .header("Authorization", "Bearer " + foreignToken)
                        .contentType("application/json")
                        .content(NON_EXISTENT_REFRESH_TOKEN))
                .andReturn();

        // Assert:现状过滤器抛 InvalidCookieException → RED;T013 白名单匿名放行后到达 controller
        String body = result.getResponse().getContentAsString();
        assertEquals(400, result.getResponse().getStatus(),
                "带旧 token 也应到达 controller;body=" + body);
        assertTrue(body.contains(SERVICE_REACHED_EVIDENCE),
                "应到达 AuthApplicationService;实际 body=" + body);
    }

    @Test
    @DisplayName("带格式非法 token(ParseException 路径)请求 refresh 端点 → 到达 controller(AC#2)")
    void malformedToken_reachesController() throws Exception {
        // Act:Bearer 后为非法 JWT 串 → SignedJWT.parse 抛 ParseException 路径
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(REFRESH_ENDPOINT)
                        .header("Authorization", "Bearer not.a.valid-jwt")
                        .contentType("application/json")
                        .content(NON_EXISTENT_REFRESH_TOKEN))
                .andReturn();

        // Assert
        String body = result.getResponse().getContentAsString();
        assertEquals(400, result.getResponse().getStatus(),
                "格式非法 token 也应到达 controller;body=" + body);
        assertTrue(body.contains(SERVICE_REACHED_EVIDENCE),
                "应到达 AuthApplicationService;实际 body=" + body);
    }
}
