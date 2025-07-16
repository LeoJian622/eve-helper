package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.*;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import java.util.HashMap;
import java.util.Map;

/**
 * ESI 舰队接口
 *
 * @author Leojan
 * date 2023-10-27 15:33
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
    public Mono<FleetDetailResponse> queryFleet(Long fleetId, String datasource, String accessesToken) {
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
    public Mono<FleetDetailResponse> updateFleet(Long fleetId, String datasource, xyz.foolcat.eve.evehelper.esi.model.send.FleetNewSetting fleetNewSetting, String accessesToken) {
        return apiClient.put().uri("/fleets/{fleet_id}/?datasource={datasource}", fleetId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(fleetNewSetting), xyz.foolcat.eve.evehelper.esi.model.send.FleetNewSetting.class)
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
    public Flux<FleetMemberResponse> queryFleetMember(Long fleetId, String datasource, String language, String accessesToken) {
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
     * 邀请加入舰队
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
    @Operation(summary = "ESI-邀请加入舰队")
    public Mono<Object> addFleetMember(Long fleetId, String datasource, xyz.foolcat.eve.evehelper.esi.model.send.FleetInvitationDetails invitation, String accessesToken) {
        return apiClient.post().uri("/fleets/{fleet_id}/members/?datasource={datasource}", fleetId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(invitation), xyz.foolcat.eve.evehelper.esi.model.send.FleetInvitationDetails.class)
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
    public Mono<Object> deleteFleetMember(Long fleetId, String datasource, Integer characterId, String accessesToken) {
        return apiClient.delete().uri("/fleets/{fleet_id}/members/{member_id}/?datasource={datasource}", fleetId, characterId, datasource)
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
    public Mono<Object> updateFleetMember(Long fleetId, String datasource, Integer characterId, xyz.foolcat.eve.evehelper.esi.model.send.FleetInvitationDetails movement, String accessesToken) {
        return apiClient.put().uri("/fleets/{fleet_id}/members/{member_id}/?datasource={datasource}", fleetId, characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(movement), xyz.foolcat.eve.evehelper.esi.model.send.FleetInvitationDetails.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 删除中队
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param squadId       中队ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "squadId", description = "中队ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-删除中队")
    public Mono<Object> deleteFleetSquad(Long fleetId, String datasource, Long squadId, String accessesToken) {
        return apiClient.delete().uri("/fleets/{fleet_id}/squads/{squad_id}/?datasource={datasource}", fleetId, squadId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 重命名中队
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param squadId       中队ID
     * @param name          中队名称
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "squadId", description = "中队ID", required = true),
            @Parameter(name = "name", description = "中队名称", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-重命名中队")
    public Mono<Object> updateFleetSquadRename(Long fleetId, String datasource, Long squadId, String name, String accessesToken) {
        Map<String, String> newName = new HashMap<>();
        newName.put("name", name);
        return apiClient.put().uri("/fleets/{fleet_id}/squads/{squad_id}/?datasource={datasource}", fleetId, squadId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(newName), Map.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 获取联队信息
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
    @Operation(summary = "ESI-获取联队信息")
    public Flux<WingResponse> queryFleetWings(Long fleetId, String datasource, String accessesToken) {
        return apiClient.get().uri("/fleets/{fleet_id}/wings/?datasource={datasource}", fleetId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(WingResponse.class);
    }

    /**
     * 添加联队
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
    @Operation(summary = "ESI-添加联队")
    public Mono<NewWingResponse> addFleetWing(Long fleetId, String datasource, String accessesToken) {
        return apiClient.post().uri("/fleets/{fleet_id}/wings/?datasource={datasource}", fleetId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(NewWingResponse.class);
    }

    /**
     * 删除联队
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param wingId        联队ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "wingId", description = "联队ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-删除联队")
    public Mono<Object> deleteFleetWing(Long fleetId, String datasource, Long wingId, String accessesToken) {
        return apiClient.delete().uri("/fleets/{fleet_id}/wings/{wing_id}/?datasource={datasource}", fleetId, wingId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 重命名联队
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param wingId        联队ID
     * @param name          联队名称
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "wingId", description = "联队名称", required = true),
            @Parameter(name = "name", description = "中队名称", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-重命名联队")
    public Mono<Object> updateFleetWingRename(Long fleetId, String datasource, Long wingId, String name, String accessesToken) {
        Map<String, String> newName = new HashMap<>();
        newName.put("name", name);
        return apiClient.put().uri("/fleets/{fleet_id}/wings/{wing_id}/?datasource={datasource}", fleetId, wingId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(newName), Map.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 添加中队
     *
     * @param fleetId       舰队ID
     * @param datasource    服务器数据源
     * @param wingId    联队ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "fleetId", description = "舰队ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "wingId", description = "联队ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-添加中队")
    public Mono<NewSquadResponse> addFleetWingSquad(Long fleetId, String datasource, Long wingId, String accessesToken) {
        return apiClient.post().uri("/fleets/{fleet_id}/wings/{wing_id}/squads/?datasource={datasource}", fleetId, wingId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(NewSquadResponse.class);
    }

}
