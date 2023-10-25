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
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 军团接口
 *
 * @author Leojan
 * @date 2023-10-25 14:55
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 军团接口")
public class CorporationApi {

    private final WebClient apiClient;

    /**
     * 军团公开信息
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-军团公开信息")
    public Mono<CorporationResponse> queryCorporationContracts(Long corporationId, String datasource) {
        return apiClient.get().uri("/corporations/{corporation_id}/?datasource={datasource}", corporationId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(CorporationResponse.class);
    }

    /**
     * 军团联盟记录
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-军团联盟记录")
    public Flux<AllianceHistoryResponse> queryCorporationAllianceHistory(Long corporationId, String datasource) {
        return apiClient.get().uri("/corporations/{corporation_id}/alliancehistory/?datasource={datasource}", corporationId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AllianceHistoryResponse.class);
    }

    /**
     * 军团拥有的蓝图
     *
     * @param corporationsId 军团ID
     * @param datasource     服务器数据源
     * @param page           页码
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationsId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团拥有的蓝图")
    public Flux<BlueprintResponse> queryCorporationBlueprints(Long corporationsId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/blueprints/?datasource={datasource}&page={page}", corporationsId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(BlueprintResponse.class);
    }

    /**
     * 军团机库容器7天审计记录
     *
     * @param corporationsId 军团ID
     * @param datasource     服务器数据源
     * @param page           页码
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationsId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团机库容器7天审计记录")
    public Flux<ContainersLogsResponse> queryCorporationContainersLogs(Long corporationsId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/containers/logs/?datasource={datasource}&page={page}", corporationsId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContainersLogsResponse.class);
    }

    /**
     * 军团部门账户名称
     *
     * @param corporationsId 军团ID
     * @param datasource     服务器数据源
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationsId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团部门账户名称")
    public Flux<DivisionNamesResponse> queryCorporationDivisions(Long corporationsId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/divisions/?datasource={datasource}", corporationsId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(DivisionNamesResponse.class);
    }

    /**
     * 军团设施
     *
     * @param corporationsId 军团ID
     * @param datasource     服务器数据源
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationsId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团设施")
    public Flux<FacilitiesResponse> queryCorporationFacilities(Long corporationsId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/facilities/?datasource={datasource}", corporationsId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(FacilitiesResponse.class);
    }

    /**
     * 军团图标地址
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-军团图标地址")
    public Mono<IconResponse> queryCorporationIcons(Long corporationId, String datasource) {
        return apiClient.get().uri("/corporations/{corporation_id}/icons/?datasource={datasource}", corporationId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(IconResponse.class);
    }

    /**
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
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
    @Operation(summary = "ESI-军团勋章信息")
    public Flux<MedalResponse> queryCorporationMedals(Long corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/medals/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MedalResponse.class);
    }

    /**
     * 军团发布的勋章信息
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
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
    @Operation(summary = "ESI-军团发布的勋章信息")
    public Flux<MedalResponse> queryCorporationIssuedMedals(Long corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/medals/issued/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MedalResponse.class);
    }

    /**
     * 军团成员列表
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
    @Operation(summary = "ESI-军团成员列表")
    public Flux<Long> queryCorporationMembers(Long corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/members/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Long.class);
    }

    /**
     * 军团成员上限
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
    @Operation(summary = "ESI-军团成员上限")
    public Mono<Integer> queryCorporationMembersLimit(Long corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/members/limit/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Integer.class);
    }

}
