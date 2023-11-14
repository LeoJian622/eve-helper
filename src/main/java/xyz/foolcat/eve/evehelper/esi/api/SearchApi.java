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
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.esi.model.SearchResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 搜索接口
 *
 * @author Leojan
 * date 2023-11-09 16:03
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 搜索接口")
public class SearchApi {

    private final WebClient apiClient;

    /**
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param categories    类别 agent, alliance, character, constellation, corporation, faction, inventory_type, region, solar_system, station, structure
     * @param searchString  搜索内容
     * @param strict        严格模式
     * @param language      语言
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "categories", description = "类别 agent, alliance, character, constellation, corporation, faction, inventory_type, region, solar_system, station, structure", required = true),
            @Parameter(name = "searchString", description = "搜索内容", required = true),
            @Parameter(name = "strict", description = "严格模式", required = true),
            @Parameter(name = "language", description = "语言", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-搜索")
    public Mono<SearchResponse> queryCharacterPlanets(Integer characterId, String datasource, String categories, String searchString, Boolean strict, String language, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/search/?datasource={datasource}&categories={categories}&search={search}&strict={strict}&language={language}", characterId, datasource, categories, searchString, strict, language)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(SearchResponse.class);
    }
}
