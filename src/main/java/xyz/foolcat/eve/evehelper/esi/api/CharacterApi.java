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
     * 查询人物公开信息
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
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
     * 人物的代理研究信息列表
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物的代理研究信息列表")
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
     * 人物蓝图清单
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物蓝图清单")
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
     * 人物雇佣记录
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物雇佣记录")
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
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param characters    人物ID列表
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "characters", description = "人物ID列表", required = true),
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

    /**
     * 跳跃疲劳时间
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-跳跃疲劳时间")
    public Mono<FatigueResponse> queryCharacterFatigue(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/fatigue/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(FatigueResponse.class);
    }

    /**
     * 人物勋章信息
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物勋章信息")
    public Flux<MedalResponse> queryCharacterMedals(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/medals/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MedalResponse.class);
    }

    /**
     * 人物通知消息
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物通知消息")
    public Flux<NotificationResponse> queryCharacterNotification(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/notifications/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(NotificationResponse.class);
    }

    /**
     * 添加到联系人通知
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-添加到联系人通知")
    public Flux<NotificationContactResponse> queryCharacterNotificationContact(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/notifications/contacts/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(NotificationContactResponse.class);
    }

    /**
     * 人物肖像图标地址
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物肖像图标地址")
    public Mono<IconResponse> queryCharacterPortrait(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/portrait/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(IconResponse.class);
    }

    /**
     * 人物角色列表
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物角色列表")
    public Mono<RoleResponse> queryCharacterRoles(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/roles/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(RoleResponse.class);
    }

    /**
     * 人物声望列表
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物声望列表")
    public Flux<StandingResponse> queryCharacterStanding(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/standings/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(StandingResponse.class);
    }


    /**
     * 人物职位列表
     *
     * @param characterId   人物ID
     * @param datasource    服务器
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-人物职位列表")
    public Flux<TitleResponse> queryCharacterTitle(Long characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/titles/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(TitleResponse.class);
    }
}