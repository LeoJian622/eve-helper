package xyz.foolcat.eve.evehelper.esi.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.EsiClientProperties;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

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
                "&code_challenge=" +
//                getCodeChallenge() +
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
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
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
