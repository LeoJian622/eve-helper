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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

/**
 * ESI 市场相关接口
 *
 * @author Leojan
 * date 2023-11-03 15:07
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 市场相关接口")
public class MarketApi {

    private final WebClient apiClient;

    private final PageTotalApi pageTotalApi;

    /**
     * 查询人物订单
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
    @Operation(summary = "ESI-查询人物订单")
    public Flux<MarketOrderResponse> queryCharacterOrders(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/orders/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MarketOrderResponse.class);
    }

    /**
     * 查询人物过去90天取消和到期的订单最大页数
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-查询人物过去90天取消和到期的订单最大页数")
    public Integer queryCharacterOrdersHistoryMaxPage(Integer characterId, String datasource, String accessesToken) {
        String uri = "/characters/" + characterId + "/orders/history/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 查询人物过去90天取消和到期的订单
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
    @Operation(summary = "ESI-查询人物过去90天取消和到期的订单")
    public Flux<MarketOrderResponse> queryCharacterOrdersHistory(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/orders/history/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MarketOrderResponse.class);
    }

    /**
     * 军团订单最大页数
     *
     * @param corporationId 军团ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团订单最大页数")
    public Integer queryCorporationOrdersMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/orders/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 查询军团订单
     *
     * @param characterId   军团ID
     * @param datasource    服务器数据源
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-查询军团订单")
    public Flux<MarketOrderResponse> queryCorporationOrders(Integer characterId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/orders/?datasource={datasource}&page={page}", characterId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MarketOrderResponse.class);
    }

    /**
     * 查询军团过去90天取消和到期的订单最大页数
     *
     * @param corporationId 军团ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-查询军团过去90天取消和到期的订单最大页数")
    public Integer queryCorporationOrdersHistoryMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/orders/history/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 查询军团过去90天取消和到期的订单
     *
     * @param characterId   军团ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-查询军团过去90天取消和到期的订单")
    public Flux<MarketOrderResponse> queryCorporationOrdersHistory(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/orders/history/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MarketOrderResponse.class);
    }

    /**
     * 查询物品星域价格历史统计
     *
     * @param regionId   星域ID
     * @param datasource 服务器数据源
     * @param typeId     物品类型ID
     * @return
     */
    @Parameters({
            @Parameter(name = "regionId", description = "星域ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "typeId", description = "物品类型ID", required = true),
    })
    @Operation(summary = "ESI-查询物品星域价格历史统计")
    public Flux<HistoricalMarketStatisticsResponse> queryMarketRegionHistory(Integer regionId, String datasource, Integer typeId) {
        return apiClient.get().uri("/markets/{region_id}/history/?datasource={datasource}&type_id={type_id}", regionId, datasource, typeId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(HistoricalMarketStatisticsResponse.class);
    }

    /**
     * 某个星域某个物品的订单最大页数
     *
     * @param regionId   星域ID
     * @param typeId     物品类型ID
     * @param datasource 服务器
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "regionId", description = "星域ID", required = true),
            @Parameter(name = "typeId", description = "物品类型ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-某个星域某个物品的订单最大页数")
    public Integer queryRegionOrdersMaxPage(Integer regionId, Integer typeId, String datasource) {
        String uri = "/markets/" + regionId + "/orders/?datasource=" + datasource + "&type_id=" + typeId + "&page=1";
        return pageTotalApi.queryMaxPage("", uri,  apiClient);
    }

    /**
     * 查询某个星域某个物品的订单
     *
     * @param regionId   星域ID
     * @param datasource 服务器数据源
     * @param typeId     物品类型ID
     * @param page       页码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "星域ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "typeId", description = "物品类型ID", required = true),
            @Parameter(name = "page", description = "页码", required = true),
    })
    @Operation(summary = "ESI-查询某个星域某个物品的订单")
    public Flux<MarketOrderResponse> queryRegionOrders(Integer regionId, String datasource, Integer typeId, Integer page) {
        return apiClient.get().uri("/markets/{region_id}/orders/?datasource={datasource}&type_id={type_id}&page={page}", regionId, datasource, typeId, page)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MarketOrderResponse.class);
    }

    /**
     * 某个星域活跃订单物品类型ID最大页数
     *
     * @param regionId   星域ID
     * @param datasource 服务器
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "regionId", description = "星域ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-某个星域活跃订单物品类型ID最大页数")
    public Integer queryRegionTypesMaxPage(Integer regionId, String datasource) {
        String uri = "/markets/" + regionId + "/types/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage("", uri,  apiClient);
    }

    /**
     * 查询某个星域活跃订单物品类型ID
     *
     * @param regionId   星域ID
     * @param datasource 服务器数据源
     * @param page       页码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "星域ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
    })
    @Operation(summary = "ESI-查询某个星域活跃订单物品类型ID")
    public Flux<Integer> queryRegionTypes(Integer regionId, String datasource, Integer page) {
        return apiClient.get().uri("/markets/{region_id}/types/?datasource={datasource}&page={page}", regionId, datasource, page)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 查询市场组ID
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
    })
    @Operation(summary = "ESI-查询市场组ID")
    public Flux<Integer> queryMarketGroup(String datasource) {
        return apiClient.get().uri("/markets/groups/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 查询市场组信息
     *
     * @param marketGroupId 物品组ID
     * @param datasource    服务器数据源
     * @param language      语言
     * @return
     */
    @Parameters({
            @Parameter(name = "marketGroupId", description = "物品组ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-查询市场组信息")
    public Mono<GroupItemResponse> queryMarketGroupInfo(Integer marketGroupId, String datasource, String language) {
        return apiClient.get().uri("/markets/groups/{market_group_id}/?datasource={datasource}&language={language}", marketGroupId, datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(GroupItemResponse.class);
    }

    /**
     * 查询价格信息
     *
     * @param datasource
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-查询价格信息")
    public Flux<PriceResponse> queryMarketPrices(String datasource) {
        return apiClient.get().uri("/markets/prices/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(PriceResponse.class);
    }
    /**
     * 建筑订单最大页数
     *
     * @param structureId   建筑ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "structureId", description = "建筑ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-建筑订单最大页数")
    public Integer queryStructureOrdersMaxPage(Long structureId, String datasource, String accessesToken) {
        String uri = "/markets/structures/" + structureId + "/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }
    /**
     * 查询建筑订单
     *
     * @param structureId   建筑ID
     * @param datasource    服务器数据源
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "structureId", description = "建筑ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-查询建筑订单")
    public Flux<MarketOrderResponse> queryStructureOrders(Long structureId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/markets/structures/{structure_id}/?datasource={datasource}&page={page}", structureId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MarketOrderResponse.class);
    }
}
