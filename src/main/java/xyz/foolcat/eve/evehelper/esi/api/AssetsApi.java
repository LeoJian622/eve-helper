package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.AssertResponse;
import xyz.foolcat.eve.evehelper.esi.model.AssetsLocationResponse;
import xyz.foolcat.eve.evehelper.esi.model.AssetsNameResponse;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

import java.util.List;

/**
 * ESI 资产相关接口
 * @author Leojan
 * date 2023-09-25 15:54
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 资产相关接口")
public class AssetsApi {

    private final WebClient apiClient;

    /**
     *
     * 获取人物资产清单
     *
     * @param characterId 人物ID
     * @param datasource 服务器
     * @param page 页码
     * @param accessesToken 授权Token
     * @return Flux<AssertResponse>
     */
    @Parameters({
            @Parameter(name = "characterId",description = "人物ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "page",description = "页码" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-人物资产清单")
    public Flux<AssertResponse> queryCharactersAssets(Integer characterId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/assets/?datasource={datasource}&page={page}",characterId,datasource,page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AssertResponse.class);
    }

    /**
     *
     * 查询人物物品位置信息
     *
     * @param characterId 人物ID
     * @param datasource 服务器
     * @param itemIds 物品ID列表
     * @param accessesToken 授权Token
     * @return Flux<AssetsLocationResponse>
     */
    @Parameters({
            @Parameter(name = "characterId",description = "人物ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "itemIds",description = "物品ID清单" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-人物物品位置信息")
    public Flux<AssetsLocationResponse> queryCharactersAssetsLocations(Integer characterId, String datasource, List<Long> itemIds, String accessesToken) {
        return apiClient.post().uri("/characters/{character_id}/assets/locations/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(itemIds),List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AssetsLocationResponse.class);
    }

    /**
     *
     * 查询人物物品命名信息
     *
     * @param characterId 人物ID
     * @param datasource 服务器
     * @param itemIds 物品ID列表
     * @param accessesToken 授权Token
     * @return Flux<AssetsNameResponse>
     */
    @Parameters({
            @Parameter(name = "characterId",description = "人物ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "itemIds",description = "物品ID清单" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-角色物品命名信息")
    public Flux<AssetsNameResponse> queryCharactersAssetsNames(Integer characterId, String datasource, List<Long> itemIds, String accessesToken) {
        return apiClient.post().uri("/characters/{character_id}/assets/names/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(itemIds),List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AssetsNameResponse.class);
    }

    /**
     *
     * 获取军团资产清单
     *
     * @param corporationId 军团ID
     * @param datasource 服务器
     * @param page 页码
     * @param accessesToken 授权Token
     * @return Flux<AssertResponse>
     */
    @Parameters({
            @Parameter(name = "corporationId",description = "军团ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "page",description = "页码" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-角色资产清单")
    public Flux<AssertResponse> queryCorporationsAssets(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/assets/?datasource={datasource}&page={page}",corporationId,datasource,page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AssertResponse.class);
    }

    /**
     *
     * 查询军团物品位置信息
     *
     * @param corporationId 军团ID
     * @param datasource 服务器
     * @param itemIds 物品ID列表
     * @param accessesToken 授权Token
     * @return Flux<AssetsLocationResponse>
     */
    @Parameters({
            @Parameter(name = "corporationId",description = "军团ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "itemIds",description = "物品ID清单" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-军团物品位置信息")
    public Flux<AssetsLocationResponse> queryCorporationsAssetsLocations(Integer corporationId, String datasource, List<Long> itemIds, String accessesToken) {
        return apiClient.post().uri("/corporations/{corporation_id}/assets/locations/?datasource={datasource}",corporationId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(itemIds),List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AssetsLocationResponse.class);
    }

    /**
     *
     * 查询军团物品命名信息
     *
     * @param corporationId 军团ID
     * @param datasource 服务器
     * @param itemIds 物品ID列表
     * @param accessesToken 授权Token
     * @return Flux<AssetsNameResponse>
     */
    @Parameters({
            @Parameter(name = "corporationId",description = "军团ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "itemIds",description = "物品ID清单" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-军团物品命名信息")
    public Flux<AssetsNameResponse> queryCorporationsAssetsNames(Integer corporationId, String datasource, List<Long> itemIds, String accessesToken) {
        return apiClient.post().uri("/corporations/{corporation_id}/assets/names/?datasource={datasource}",corporationId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(itemIds),List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AssetsNameResponse.class);
    }
}
