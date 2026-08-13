package xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientProperties;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ErrorResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * @author Leojan
 * date 2023-08-02 9:07
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizeOAuth {

    private static final int LEN = 128;
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~";
    private static final SecureRandom RND = new SecureRandom();
    private static final String URI_AUTHENTICATION = "/authorize";
    private static final String URI_ACCESS_TOKEN = "/token";

    private final EsiClientProperties esiClientProperties;

    private final WebClient authClient;

    public static final String AGENT = "eve-helper";

    public String authorizeUrl(Set<String> scopes) {
        return esiClientProperties.getAuthUrl() +
                URI_AUTHENTICATION +
                "?" +
                "response_type=" +
                encode("code") +
                "&redirect_uri=" +
                encode(esiClientProperties.getCallBackUrl()) +
                "&client_id=" +
                encode(esiClientProperties.getClientId()) +
                "&scope=" +
                encode(String.join(" ", scopes)) +
                "&state=" +
                encode(AGENT) +
                "&device_id=" +
                encode(AGENT) +
                // 晨曦(Serenity)OAuth 兼容:code_challenge 有意留空(不启用 PKCE)。
                // 若启用 PKCE,晨曦授权服务器会拒绝「授权码换取 token」流程,故此处刻意不调用 getCodeChallenge()。
                "&code_challenge=" +
                "&code_challenge_method=" +
                encode("S256");
    }

    /**
     * OAuth 认证
     * @param grantType 认证类型
     * @param token 授权字符
     * @return Mono<AuthTokenResponse>
     */
    public Mono<AuthTokenResponse> updateAccessToken(GrantType grantType, String token) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("grant_type", grantType.toString());
        parameters.add("client_id", esiClientProperties.getClientId());
        if (GrantType.AUTHORIZATION_CODE == grantType) {
            parameters.add("code", token);
        } else if (GrantType.REFRESH_TOKEN == grantType) {
            parameters.add("refresh_token", token);
        }

        return authClient.post()
                .uri(URI_ACCESS_TOKEN)
                .bodyValue(parameters)
                .retrieve()
                // 上游原始错误文本(如 invalid_grant:Invalid refresh token. Token has been revoked.)
                // 只进服务端日志,不进异常 message:否则经 GlobalExceptionHandler 原样下发给客户端,
                // 既泄露 ESI 内部细节,又使「本人角色但 token 已废」与归属校验失败可被区分(006 FR-020)
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> {
                            log.warn("ESI 认证失败(4xx): grantType={}, error={}, description={}",
                                    grantType, res.getError(), res.getErrorDescription());
                            return Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> {
                            log.error("ESI 服务异常(5xx): grantType={}, error={}, description={}",
                                    grantType, res.getError(), res.getErrorDescription());
                            return Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE));
                        }))
                .bodyToMono(AuthTokenResponse.class);
    }

    private static String encode(String parameter) {
        return URLEncoder.encode(parameter, StandardCharsets.UTF_8);
    }

    private String getCodeChallenge() {
        try {
            StringBuilder sb = new StringBuilder(LEN);
            for (int i = 0; i < LEN; i++) {
                sb.append(AB.charAt(RND.nextInt(AB.length())));
            }
            String codeVerifier = sb.toString();
            byte[] ascii = codeVerifier.getBytes(StandardCharsets.US_ASCII);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] sha = digest.digest(ascii);
            return Base64.getUrlEncoder().encodeToString(sha);
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

}
