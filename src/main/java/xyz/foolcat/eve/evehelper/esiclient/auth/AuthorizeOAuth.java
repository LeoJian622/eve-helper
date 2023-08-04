package xyz.foolcat.eve.evehelper.esiclient.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esiclient.EsiClientProperties;
import xyz.foolcat.eve.evehelper.esiclient.Model.AuthErrorResponse;
import xyz.foolcat.eve.evehelper.esiclient.Model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * @author Leojan
 * @date 2023-08-02 9:07
 */

@Component
@RequiredArgsConstructor
public class AuthorizeOAuth {

    private final EsiClientProperties esiClientProperties;

    private final WebClient authClient;

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
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(parameters)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getError_description()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getError_description()))))
                .bodyToMono(AuthTokenResponse.class);
    }

}
