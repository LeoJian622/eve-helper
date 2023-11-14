package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.esi.model.LoyaltyPointsResponse;
import xyz.foolcat.eve.evehelper.esi.model.OfferResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 忠诚点接口
 *
 * @author Leojan
 * date 2023-10-31 16:04
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 忠诚点接口")
public class LoyaltyApi {

    private final WebClient apiClient;

    /**
     * 人物的忠诚度点列表
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物的忠诚度点列表")
    public Flux<LoyaltyPointsResponse> queryCharacterLoyaltyPoints(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/loyalty/points/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(LoyaltyPointsResponse.class);
    }

    /**
     * 忠诚点商店兑换列表
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-忠诚点商店兑换列表")
    public Flux<OfferResponse> queryCorporationLoyaltyPoints(Integer corporationId, String datasource) {
        return apiClient.get().uri("/loyalty/stores/{corporation_id}/offers/?datasource={datasource}", corporationId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(OfferResponse.class);
    }
}
