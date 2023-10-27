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
import xyz.foolcat.eve.evehelper.esi.model.*;
import xyz.foolcat.eve.evehelper.esi.model.send.FleetInvitationDetails;
import xyz.foolcat.eve.evehelper.esi.model.send.FleetNewSetting;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 舰队接口
 *
 * @author Leojan
 * @date 2023-10-27 15:33
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 舰队接口")
public class FleetApi {

    private final WebClient apiClient;

    /**
     * 人物舰队信息
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
    @Operation(summary = "ESI-人物舰队信息")
    public Mono<CharacterFleetResponse> queryCharacterFittings(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/fleet/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(CharacterFleetResponse.class);
    }

    /**
     * 舰队登记详细信息
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-舰队登记详细信息")
    public Mono<FleetDetailResponse> queryFleet(Integer fleetId, String datasource, String accessesToken) {
        return apiClient.get().uri("/fleets/{fleet_id}/?datasource={datasource}", fleetId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(FleetDetailResponse.class);
    }

    /**
     * 更新舰队登记信息
     *
     * @param fleetId         舰队ID
     * @param datasource      服务器数据源
     * @param fleetNewSetting 舰队信息设置
     * @param accessesToken   授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "fleetNewSetting", description = "舰队信息设置", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-更新舰队登记信息")
    public Mono<FleetDetailResponse> updateFleet(Integer fleetId, String datasource, FleetNewSetting fleetNewSetting, String accessesToken) {
        return apiClient.put().uri("/fleets/{fleet_id}/?datasource={datasource}", fleetId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(fleetNewSetting), FleetNewSetting.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(FleetDetailResponse.class);
    }

    /**
     * 舰队成员
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param language      语言
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-舰队成员")
    public Flux<FleetMemberResponse> queryFleetMember(Integer fleetId, String datasource, String language, String accessesToken) {
        return apiClient.get().uri("/fleets/{fleet_id}/members/?datasource={datasource}&language={language}", fleetId, datasource, language)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(FleetMemberResponse.class);
    }

    /**
     * 舰队成员
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param invitation    舰队邀请信息
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "invitation", description = "舰队邀请信息", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-舰队成员")
    public Mono<Object> addFleetMember(Integer fleetId, String datasource, FleetInvitationDetails invitation, String accessesToken) {
        return apiClient.post().uri("/fleets/{fleet_id}/members/?datasource={datasource}&language={language}", fleetId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(invitation), FleetInvitationDetails.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 踢出舰队成员
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param characterId   人物ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-踢出舰队成员")
    public Mono<Object> deleteFleetMember(Integer fleetId, String datasource, Integer characterId, String accessesToken) {
        return apiClient.delete().uri("/fleets/{fleet_id}/members/{member_id}/?datasource={datasource}&language={language}", fleetId, characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 移动或授予舰队成员职位
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param characterId   人物ID
     * @param movement      移动信息
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "movement", description = "移动信息", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-移动或授予舰队成员职位")
    public Mono<Object> updateFleetMember(Integer fleetId, String datasource, Integer characterId, FleetInvitationDetails movement, String accessesToken) {
        return apiClient.put().uri("/fleets/{fleet_id}/members/{member_id}/?datasource={datasource}&language={language}", fleetId, characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(movement), FleetInvitationDetails.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

}
