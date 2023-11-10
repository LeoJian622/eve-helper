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
 * ESI 行星开发接口
 *
 * @author Leojan
 * @date 2023-11-07 14:18
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 机遇系统接口")
public class PlanetaryInteractionApi {

    private final WebClient apiClient;

    /**
     * 查询人物拥有的所有行星殖民地列表
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
    @Operation(summary = "ESI-查询人物拥有的所有行星殖民地列表")
    public Flux<ColonyResponse> queryCharacterPlanets(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/planets/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ColonyResponse.class);
    }

    /**
     * 查询人物行星殖民地布局的全部详细信息，包括链接、插针和路线
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param planetId      行星ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "planetId", description = "行星ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-查询人物行星殖民地布局的全部详细信息")
    public Mono<ColonyLayoutResponse> queryCharacterPlanet(Integer characterId, String datasource, Integer planetId, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/planets/{planet_id}/?datasource={datasource}", characterId, planetId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(ColonyLayoutResponse.class);
    }

    /**
     * 查询军团的海关信息及配置
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-查询军团的海关信息及配置")
    public Flux<CustomsOfficesSettingResponse> queryCorporationCustomsOffices(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/customs_offices/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(CustomsOfficesSettingResponse.class);
    }

    /**
     * 查询行星工厂生产详细信息
     *
     * @param schematicId 工厂流程ID
     * @param datasource  服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "schematicId", description = "工厂流程ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-行星工厂生产详细信息")
    public Mono<FactorySchematicResponse> queryUniverseSchematic(Integer schematicId, String datasource) {
        return apiClient.get().uri("/universe/schematics/{schematic_id}/?datasource={datasource}", schematicId,datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(FactorySchematicResponse.class);
    }
}
