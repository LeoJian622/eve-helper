package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientConstants;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CharacterAttributesResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.CharacterSkillResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.SkillQueueResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiStatusUtil;

import java.util.List;

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

    private final WebClient esiClient;

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
        return esiClient.get().uri("/characters/{character_id}/attributes/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
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
        return esiClient.get().uri("/characters/{character_id}/skillqueue/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
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
        return esiClient.get().uri("/characters/{character_id}/skills/?datasource={datasource}",characterId,datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())
                .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil.dataError())
                .bodyToMono(CharacterSkillResponse.class);
    }

    /**
     * 人物技能信息(含响应头,供缓存 TTL 提取)。
     * <p>022/T011 新增:与 {@link #queryCharactersSkills} 同一 URI,改用 exchangeToMono 保留响应头。
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 含响应头的 ResponseEntity
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物技能信息(含响应头)")
    public Mono<ResponseEntity<CharacterSkillResponse>> queryCharactersSkillsResponse(Integer characterId, String datasource, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/skills/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<CharacterSkillResponse>() {}, "ESI 人物技能"))
                .timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
    }

    /**
     * 人物属性信息(含响应头,供缓存 TTL 提取)。
     * <p>022/T011 新增:与 {@link #queryCharactersAttributes} 同一 URI,改用 exchangeToMono 保留响应头。
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 含响应头的 ResponseEntity
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物属性信息(含响应头)")
    public Mono<ResponseEntity<CharacterAttributesResponse>> queryCharactersAttributesResponse(Integer characterId, String datasource, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/attributes/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<CharacterAttributesResponse>() {}, "ESI 人物属性"))
                .timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
    }

    /**
     * 人物技能队列(含响应头,供缓存 TTL 提取;Flux 端点变体返回 List)。
     * <p>022/T011 新增:与 {@link #queryCharactersSkillqueue} 同一 URI,改用 exchangeToMono 保留响应头,
     * 整体取出为 {@code List<SkillQueueResponse>} 以便缓存回填。
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 含响应头的 ResponseEntity(List 体)
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物技能队列(含响应头)")
    public Mono<ResponseEntity<List<SkillQueueResponse>>> queryCharactersSkillqueueResponse(Integer characterId, String datasource, String accessesToken) {
        return esiClient.get().uri("/characters/{character_id}/skillqueue/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
                        response, new ParameterizedTypeReference<List<SkillQueueResponse>>() {}, "ESI 人物技能队列"))
                .timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
    }
}
