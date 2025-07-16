package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.FittingResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

/**
 * ESI 舰船装配接口
 *
 * @author Leojan
 * date 2023-10-27 14:27
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 舰船装配接口")
public class FittingApi {

    private final WebClient apiClient;

    /**
     * 人物舰船装配信息
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
    @Operation(summary = "ESI-人物舰船装配信息")
    public Flux<FittingResponse> queryCharacterFittings(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/fittings/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(FittingResponse.class);
    }

    /**
     * 添加人物舰船装配信息
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
    @Operation(summary = "ESI-添加人物舰船装配信息")
    public Mono<FittingResponse> addCharacterFittings(Integer characterId, String datasource, xyz.foolcat.eve.evehelper.esi.model.send.Fitting fitting, String accessesToken) {
        return apiClient.post().uri("/characters/{character_id}/fittings/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(fitting), xyz.foolcat.eve.evehelper.esi.model.send.Fitting.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(FittingResponse.class);
    }

    /**
     * 删除人物舰船装配信息
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param fittingId     装配ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "fittingId", description = "装配ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-删除人物舰船装配信息")
    public Mono<Object> deleteCharacterFittings(Integer characterId, String datasource, Integer fittingId, String accessesToken) {
        return apiClient.delete().uri("/characters/{character_id}/fittings/{fitting_id}/?datasource={datasource}", characterId, fittingId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }
}
