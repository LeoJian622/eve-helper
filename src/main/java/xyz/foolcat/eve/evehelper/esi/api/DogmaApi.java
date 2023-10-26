package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.DogmaAttributeResponse;
import xyz.foolcat.eve.evehelper.esi.model.DogmaEffectResponse;
import xyz.foolcat.eve.evehelper.esi.model.DynamicItemResponse;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 属性接口
 *
 * @author Leojan
 * @date 2023-10-26 15:15
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 属性接口")
public class DogmaApi {

    private final WebClient apiClient;

    /**
     * 属性ID列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-属性ID列表")
    public Flux<Integer> queryAttributes(String datasource) {
        return apiClient.get().uri("/dogma/attributes/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 属性详情
     *
     * @param attributeId 属性ID
     * @param datasource  服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "attributeId", description = "属性ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-属性详情")
    public Mono<DogmaAttributeResponse> queryAttribute(Integer attributeId, String datasource) {
        return apiClient.get().uri("/dogma/attributes/{attribute_id}/?datasource={datasource}", attributeId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(DogmaAttributeResponse.class);
    }

    /**
     * 深渊装备属性详情
     *
     * @param typeId     物品名ID
     * @param itemId     物品ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "typeId", description = "物品名ID", required = true),
            @Parameter(name = "itemId", description = "物品ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-深渊装备属性详情")
    public Mono<DynamicItemResponse> queryAttribute(Integer typeId, Integer itemId, String datasource) {
        return apiClient.get().uri("/dogma/dynamic/items/{type_id}/{item_id}/?datasource={datasource}", typeId, itemId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(DynamicItemResponse.class);
    }

    /**
     * 属性影响ID列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-属性影响ID列表")
    public Flux<Integer> queryEffects(String datasource) {
        return apiClient.get().uri("/dogma/effects/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 深渊属性影响详情
     *
     * @param effectId   属性影响ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "effectId", description = "属性影响ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-深渊属性影响详情")
    public Flux<DogmaEffectResponse> queryEffect(Integer effectId, String datasource) {
        return apiClient.get().uri("/dogma/effects/{effect_id}/?datasource={datasource}", effectId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(DogmaEffectResponse.class);
    }


}
