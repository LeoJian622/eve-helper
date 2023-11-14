package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.CharacterAttributesResponse;
import xyz.foolcat.eve.evehelper.esi.model.CharacterSkillResponse;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.esi.model.SkillQueueResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 人物技能接口
 *
 * @author Leojan
 * date 2023-11-14 11:12
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 人物技能接口")
public class SkillApi {

    private final WebClient apiClient;

    /**
     *
     * 人物属性相关信息
     *
     * @param characterId 人物ID
     * @param datasource 服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId",description = "人物ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-人物属性相关信息")
    public Mono<CharacterAttributesResponse> queryCharactersAttributes(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/attributes/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(CharacterAttributesResponse.class);
    }

    /**
     *
     * 人物属性相关信息
     *
     * @param characterId 人物ID
     * @param datasource 服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId",description = "人物ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-人物属性相关信息")
    public Flux<SkillQueueResponse> queryCharactersSkillqueue(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/skillqueue/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(SkillQueueResponse.class);
    }

    /**
     *
     * 人物技能信息
     *
     * @param characterId 人物ID
     * @param datasource 服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId",description = "人物ID" ,required = true),
            @Parameter(name = "datasource",description = "服务器数据源" ,required = true),
            @Parameter(name = "accessesToken",description = "授权Token" ,required = true),
    })
    @Operation(summary = "ESI-人物属性相关信息")
    public Mono<CharacterSkillResponse> queryCharactersSkills(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/skills/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(CharacterSkillResponse.class);
    }
}
