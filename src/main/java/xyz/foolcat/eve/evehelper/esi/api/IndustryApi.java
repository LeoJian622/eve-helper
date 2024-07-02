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
import xyz.foolcat.eve.evehelper.esi.model.*;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 工业生产接口
 *
 * @author Leojan
 * date 2023-10-30 15:54
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 工业生产")
public class IndustryApi {

    private final WebClient apiClient;

    private final PageTotalApi pageTotalApi;

    /**
     * 人物工业生产项目
     *
     * @param characterId      人物ID
     * @param datasource       服务器
     * @param includeCompleted 是否包含完成的项目
     * @param accessesToken    授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "includeCompleted", description = "是否包含完成的项目", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-工业生产项目")
    public Flux<IndustryJobPlacedResponse> queryCharacterIndustryJobs(Integer characterId, String datasource, Boolean includeCompleted, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/industry/jobs/?datasource={datasource}&include_completed={include_completed}", characterId, datasource, includeCompleted)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(IndustryJobPlacedResponse.class);
    }

    /**
     * 人物采矿明细记录最大页数
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
    @Operation(summary = "ESI-人物采矿明细记录最大页数")
    public Integer queryCharacterMiningMaxPage(Integer characterId, String datasource, String accessesToken) {
        String uri = "/characters/" + characterId + "/mining/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri, apiClient);
    }


    /**
     * 人物采矿明细记录
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物采矿明细记录")
    public Flux<MiningLedgerResponse> queryCharacterMining(Integer characterId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/mining/?datasource={datasource}&page={page}", characterId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MiningLedgerResponse.class);
    }

    /**
     * 开采碎裂计时最大页数
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
    @Operation(summary = "ESI-开采碎裂计时最大页数")
    public Integer queryCorporationMiningExtractionsMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporation/" + corporationId + "/mining/extractions/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri, apiClient);
    }

    /**
     * 开采碎裂计时
     *
     * @param corporationId 军团ID
     * @param datasource    服务器
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-开采碎裂计时")
    public Flux<ChunkTimersResponse> queryCorporationMiningExtractions(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporation/{corporation_id}/mining/extractions/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ChunkTimersResponse.class);
    }

    /**
     * 军团开采记录最大页数
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
    @Operation(summary = "ESI-军团开采记录最大页数")
    public Integer queryCorporationMiningObserversMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporation/" + corporationId + "/mining/observers/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri, apiClient);
    }

    /**
     * 军团开采记录
     *
     * @param corporationId 军团ID
     * @param datasource    服务器
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团开采记录")
    public Flux<CorporationObserverResponse> queryCorporationMiningObservers(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporation/{corporation_id}/mining/observers/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(CorporationObserverResponse.class);
    }

    /**
     * 军团单个建筑采矿明细最大页数
     *
     * @param corporationId 军团ID
     * @param observerId    工业建筑ID
     * @param datasource    服务器
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "observerId", description = "工业建筑ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团单个建筑采矿明细最大页数")
    public Integer queryCorporationMiningObserverMaxPage(Integer corporationId, Long observerId, String datasource, String accessesToken) {
        String uri = "/corporation/" + corporationId + "/mining/observers/" + observerId + "/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri, apiClient);
    }

    /**
     * 军团单个建筑采矿明细
     *
     * @param corporationId 军团ID
     * @param datasource    服务器
     * @param observerId    工业建筑ID
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "observerId", description = "工业建筑ID", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团单个建筑采矿明细")
    public Flux<ObserverMiningLedgerResponse> queryCorporationMiningObserver(Integer corporationId, String datasource, Long observerId, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporation/{corporation_id}/mining/observers/{observer_id}/?datasource={datasource}&page={page}", corporationId, observerId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ObserverMiningLedgerResponse.class);
    }

    /**
     * 军团工业生产项目最大页数
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
    @Operation(summary = "ESI-军团工业生产项目最大页数")
    public Integer queryCorporationIndustryJobsMaxPage(Integer corporationId, String datasource, Boolean includeCompleted, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/industry/jobs/?datasource=" + datasource + "&include_completed=" + includeCompleted + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri, apiClient);
    }

    /**
     * 军团工业生产项目
     *
     * @param corporationId    军团ID
     * @param datasource       服务器
     * @param includeCompleted 是否包含完成的项目
     * @param accessesToken    授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "includeCompleted", description = "是否包含完成的项目", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团工业生产项目")
    public Flux<IndustryJobPlacedResponse> queryCorporationIndustryJobs(Integer corporationId, String datasource, Boolean includeCompleted, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/industry/jobs/?datasource={datasource}&include_completed={include_completed}", corporationId, datasource, includeCompleted)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(IndustryJobPlacedResponse.class);
    }


    /**
     * 工业设施清单
     *
     * @param datasource 服务器
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-军团工业生产项目")
    public Flux<FacilitiesResponse> queryIndustryFacilities(String datasource) {
        return apiClient.get().uri("/industry/facilities/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(FacilitiesResponse.class);
    }

    /**
     * 星系工业成本系数
     *
     * @param datasource 服务器
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-星系工业成本系数")
    public Flux<CostIndiciesResponse> queryIndustrySystems(String datasource) {
        return apiClient.get().uri("/industry/systems/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(CostIndiciesResponse.class);
    }
}
