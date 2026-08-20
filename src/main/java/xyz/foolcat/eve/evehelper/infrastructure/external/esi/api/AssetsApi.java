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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiStatusUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AssetResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AssetsLocationResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AssetsNameResponse;

import java.time.Duration;
import java.util.List;

/**
 * ESI 资产相关接口
 * @author Leojan
 * date 2023-09-25 15:54
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Tag(name = "ESI 资产相关接口")
public class AssetsApi {

    private final WebClient esiClient;

    private final PageTotalApi pageTotalApi;

    /**
     * ESI 数据接口调用超时(宪法:外部调用 5s 超时)
     */
    private static final Duration ESI_DATA_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 人物资产清单最大页数
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
    @Operation(summary = "ESI-人物资产清单最大页数")
    public Integer queryCharactersAssetsMaxPage(Integer characterId, String datasource, String accessesToken) {
        String uri = "/characters/" + characterId + "/assets/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  esiClient);
    }

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
    public Flux<AssetResponse> queryCharactersAssets(Integer characterId, String datasource, Integer page, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/assets/?datasource={datasource}&page={page}",characterId,datasource,page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(AssetResponse.class);
    }

    /**
     * 获取人物资产清单(含响应头)
     *
     * <p>015/T006:P4 试点。用 {@code exchangeToMono} + {@code toEntity(ParameterizedTypeReference)}
     * 一次取出含响应头(如 {@code Cache-Control: max-age})的 {@link ResponseEntity},
     * 供后续任务(T008)解析缓存 TTL。4xx/5xx 分支复刻 {@link PageTotalApi#queryMaxPage} 先例。</p>
     *
     * @param characterId 人物ID
     * @param datasource  服务器
     * @param page        页码
     * @param accessesToken 授权Token
     * @return ResponseEntity&lt;List&lt;AssetResponse&gt;&gt;
     */
    @Parameters({
            @Parameter(name = "characterId",description = "人物ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "page",description = "页码" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-人物资产清单(含响应头)")
    public Mono<ResponseEntity<List<AssetResponse>>> queryCharactersAssetsResponse(Integer characterId, String datasource, Integer page, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/assets/?datasource={datasource}&page={page}",characterId,datasource,page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> {
                    HttpStatusCode httpStatusCode = response.statusCode();
                    if (httpStatusCode.is4xxClientError()) {
                        if (EsiStatusUtil.isForbidden(httpStatusCode)) {
                            log.warn("ESI 人物资产权限不足(403): characterId={}", characterId);
                            return Mono.error(EsiStatusUtil.forbiddenAgent());
                        }
                        return Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));
                    }
                    if (httpStatusCode.is5xxServerError()) {
                        log.warn("ESI 人物资产服务端错误(5xx): characterId={}, status={}", characterId, httpStatusCode.value());
                        return Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE));
                    }
                    return response.toEntity(new ParameterizedTypeReference<List<AssetResponse>>() {
                    });
                }).timeout(ESI_DATA_TIMEOUT);
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
        return esiClient.post().uri("/characters/{character_id}/assets/locations/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(itemIds),List.class)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
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
        return esiClient.post().uri("/characters/{character_id}/assets/names/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(itemIds),List.class)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(AssetsNameResponse.class);
    }

    /**
     * 军团资产清单最大页数
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
    @Operation(summary = "ESI-军团资产清单最大页数")
    public Integer queryCorporationsAssetsMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/assets/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  esiClient);
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
    @Operation(summary = "ESI-军团资产清单")
    public Flux<AssetResponse> queryCorporationsAssets(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return esiClient.get().uri("/corporations/{corporation_id}/assets/?datasource={datasource}&page={page}",corporationId,datasource,page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(AssetResponse.class);
    }

    /**
     * 获取军团资产清单(含响应头)
     *
     * <p>015/T006:P4 试点。用 {@code exchangeToMono} + {@code toEntity(ParameterizedTypeReference)}
     * 一次取出含响应头(如 {@code Cache-Control: max-age})的 {@link ResponseEntity},
     * 供后续任务(T008)解析缓存 TTL。4xx/5xx 分支复刻 {@link PageTotalApi#queryMaxPage} 先例。</p>
     *
     * @param corporationId 军团ID
     * @param datasource    服务器
     * @param page          页码
     * @param accessesToken 授权Token
     * @return ResponseEntity&lt;List&lt;AssetResponse&gt;&gt;
     */
    @Parameters({
            @Parameter(name = "corporationId",description = "军团ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "page",description = "页码" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-军团资产清单(含响应头)")
    public Mono<ResponseEntity<List<AssetResponse>>> queryCorporationsAssetsResponse(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return esiClient.get().uri("/corporations/{corporation_id}/assets/?datasource={datasource}&page={page}",corporationId,datasource,page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> {
                    HttpStatusCode httpStatusCode = response.statusCode();
                    if (httpStatusCode.is4xxClientError()) {
                        if (EsiStatusUtil.isForbidden(httpStatusCode)) {
                            log.warn("ESI 军团资产权限不足(403): corporationId={}", corporationId);
                            return Mono.error(EsiStatusUtil.forbiddenAgent());
                        }
                        return Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));
                    }
                    if (httpStatusCode.is5xxServerError()) {
                        log.warn("ESI 军团资产服务端错误(5xx): corporationId={}, status={}", corporationId, httpStatusCode.value());
                        return Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE));
                    }
                    return response.toEntity(new ParameterizedTypeReference<List<AssetResponse>>() {
                    });
                }).timeout(ESI_DATA_TIMEOUT);
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
        return esiClient.post().uri("/corporations/{corporation_id}/assets/locations/?datasource={datasource}",corporationId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(itemIds),List.class)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
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
        return esiClient.post().uri("/corporations/{corporation_id}/assets/names/?datasource={datasource}",corporationId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(itemIds),List.class)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToFlux(AssetsNameResponse.class);
    }
}
