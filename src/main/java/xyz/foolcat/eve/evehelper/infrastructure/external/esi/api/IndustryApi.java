package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientConstants;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiStatusUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.*;

import java.util.List;

/**
 * ESI 工业生产接口
 *
 * @author Leojan
 * date 2023-10-30 15:54
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Tag(name = "ESI 工业生产")
public class IndustryApi {

    private final WebClient esiClient;

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
        return esiClient.get().uri("/characters/{character_id}/industry/jobs/?datasource={datasource}&include_completed={include_completed}", characterId, datasource, includeCompleted)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(IndustryJobPlacedResponse.class);
    }

    /**
     * 人物工业生产项目(含响应头,015/T009 复制 T008 模式)。
     *
     * <p>用 {@code exchangeToMono} + {@code toEntity(ParameterizedTypeReference)} 一次取出
     * 含响应头(如 {@code Cache-Control: max-age})的 {@link ResponseEntity},
     * 供包裹层解析缓存 TTL。4xx/5xx 分支复刻 {@link PageTotalApi#queryMaxPage} 先例。</p>
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "includeCompleted", description = "是否包含完成的项目", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物工业生产项目(含响应头)")
    public Mono<ResponseEntity<List<IndustryJobPlacedResponse>>> queryCharacterIndustryJobsResponse(Integer characterId, String datasource, Boolean includeCompleted, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/industry/jobs/?datasource={datasource}&include_completed={include_completed}", characterId, datasource, includeCompleted)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<List<IndustryJobPlacedResponse>>() {}, "ESI 人物工业任务")).timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
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
        return pageTotalApi.queryMaxPage(accessesToken, uri, esiClient);
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
        return esiClient.get().uri("/characters/{character_id}/mining/?datasource={datasource}&page={page}", characterId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
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
        return pageTotalApi.queryMaxPage(accessesToken, uri, esiClient);
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
        return esiClient.get().uri("/corporation/{corporation_id}/mining/extractions/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
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
        return pageTotalApi.queryMaxPage(accessesToken, uri, esiClient);
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
        return esiClient.get().uri("/corporation/{corporation_id}/mining/observers/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
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
        return pageTotalApi.queryMaxPage(accessesToken, uri, esiClient);
    }

    /**
     * 军团单个建筑采矿明细最大页数(含响应头,015/T010 maxPage TTL 来源通路)。
     *
     * <p>调 {@link PageTotalApi#queryMaxPageWithHeaders} 同时取最大页数与响应头,
     * 供包裹层为 maxPage 计算缓存 TTL。原 {@link #queryCorporationMiningObserverMaxPage} 保留、零破坏。</p>
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "observerId", description = "工业建筑ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团单个建筑采矿明细最大页数(含响应头)")
    public PageTotalApi.PageCountResult queryCorporationMiningObserverMaxPageResponse(Integer corporationId, String datasource, Long observerId, String accessesToken) {
        String uri = "/corporation/" + corporationId + "/mining/observers/" + observerId + "/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPageWithHeaders(accessesToken, uri, esiClient);
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
        return esiClient.get().uri("/corporation/{corporation_id}/mining/observers/{observer_id}/?datasource={datasource}&page={page}", corporationId, observerId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(ObserverMiningLedgerResponse.class);
    }

    /**
     * 军团单个建筑采矿明细(含响应头,015/T010 复制 T008 模式)。
     *
     * <p>用 {@code exchangeToMono} + {@code toEntity(ParameterizedTypeReference)} 一次取出
     * 含响应头(如 {@code Cache-Control: max-age})的 {@link ResponseEntity},
     * 供包裹层解析缓存 TTL。4xx/5xx 分支复刻 {@link PageTotalApi#queryMaxPage} 先例。</p>
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "observerId", description = "工业建筑ID", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团单个建筑采矿明细(含响应头)")
    public Mono<ResponseEntity<List<ObserverMiningLedgerResponse>>> queryCorporationMiningObserverResponse(Integer corporationId, String datasource, Long observerId, Integer page, String accessesToken) {
        return esiClient.get().uri("/corporation/{corporation_id}/mining/observers/{observer_id}/?datasource={datasource}&page={page}", corporationId, observerId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<List<ObserverMiningLedgerResponse>>() {}, "ESI 军团单建筑采矿")).timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
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
        return pageTotalApi.queryMaxPage(accessesToken, uri, esiClient);
    }

    /**
     * 军团工业生产项目最大页数(含响应头,015/T009 maxPage TTL 来源通路)。
     *
     * <p>调 {@link PageTotalApi#queryMaxPageWithHeaders} 同时取最大页数与响应头
     * (含 {@code Cache-Control: max-age}),供包裹层为 maxPage 计算缓存 TTL。
     * 原 {@link #queryCorporationIndustryJobsMaxPage} 保留、零破坏。</p>
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "includeCompleted", description = "是否包含完成的项目", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团工业生产项目最大页数(含响应头)")
    public PageTotalApi.PageCountResult queryCorporationIndustryJobsMaxPageResponse(Integer corporationId, String datasource, Boolean includeCompleted, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/industry/jobs/?datasource=" + datasource + "&include_completed=" + includeCompleted + "&page=1";
        return pageTotalApi.queryMaxPageWithHeaders(accessesToken, uri, esiClient);
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
        return esiClient.get().uri("/corporations/{corporation_id}/industry/jobs/?datasource={datasource}&include_completed={include_completed}", corporationId, datasource, includeCompleted)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(IndustryJobPlacedResponse.class);
    }

    /**
     * 军团工业生产项目(含响应头,015/T009 复制 T008 模式)。
     *
     * <p>同 {@link #queryCharacterIndustryJobsResponse}:一次取出含响应头 {@link ResponseEntity},
     * 供包裹层解析缓存 TTL。4xx/5xx 分支复刻 {@link PageTotalApi#queryMaxPage} 先例。</p>
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "includeCompleted", description = "是否包含完成的项目", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团工业生产项目(含响应头)")
    public Mono<ResponseEntity<List<IndustryJobPlacedResponse>>> queryCorporationIndustryJobsResponse(Integer corporationId, String datasource, Boolean includeCompleted, String accessesToken) {
        return esiClient.get().uri("/corporations/{corporation_id}/industry/jobs/?datasource={datasource}&include_completed={include_completed}", corporationId, datasource, includeCompleted)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<List<IndustryJobPlacedResponse>>() {}, "ESI 军团工业任务")).timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
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
        return esiClient.get().uri("/industry/facilities/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
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
        return esiClient.get().uri("/industry/systems/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(CostIndiciesResponse.class);
    }
}
