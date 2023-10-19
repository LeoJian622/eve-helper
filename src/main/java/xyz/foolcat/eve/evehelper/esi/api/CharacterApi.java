package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.*;
import xyz.foolcat.eve.evehelper.exception.EsiException;

import java.util.List;


/**
 * ESI 人物相关接口
 *
 * @author Leojan
 * @date 2023-10-18 16:57
 */

@Service
@RequiredArgsConstructor
public class CharacterApi {

    private final WebClient apiClient;

    /**
     * 查询角色公开信息
     *
     * @param characterId   角色ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "角色ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物详细公开信息")
    public Mono<CharacterPublicInfoResponse> queryCharacter(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(CharacterPublicInfoResponse.class);
    }

    /**
     * 角色的代理研究信息列表
     *
     * @param characterId   角色ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "角色ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-角色的代理研究信息列表")
    public Flux<AgentsResearchResponse> queryCharacterAgentsResearch(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/agents_research/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AgentsResearchResponse.class);
    }

    /**
     * 角色蓝图清单
     *
     * @param characterId   角色ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "角色ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-角色蓝图清单")
    public Flux<BlueprintResponse> queryCharacterBlueprint(Long characterId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/blueprints/?datasource={datasource}&page={page}", characterId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(BlueprintResponse.class);
    }

    /**
     * 角色雇佣记录
     *
     * @param characterId   角色ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "角色ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-角色雇佣记录")
    public Flux<CorporationHistoryResponse> queryCharacterCorporationHistory(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/corporationhistory/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(CorporationHistoryResponse.class);
    }

    /**
     * CSPA花费
     *
     * @param characterId   角色ID
     * @param datasource    服务器
     * @param characters    角色ID列表
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "角色ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "characters", description = "角色ID列表", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-CSPA花费")
    public Mono<Float> queryCharacterCspa(Long characterId, String datasource, List<Integer> characters, String accessesToken) {
        return apiClient.post().uri("/characters/{character_id}/cspa/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(characters), List.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Float.class);
    }
}