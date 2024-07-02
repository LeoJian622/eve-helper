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
 * date 2023-10-25 14:55
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 军团接口")
public class CorporationApi {

    private final WebClient apiClient;

    private final PageTotalApi pageTotalApi;

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
    public Mono<CorporationResponse> queryCorporation(Integer corporationId, String datasource) {
        return apiClient.get().uri("/corporations/{corporation_id}/?datasource={datasource}", corporationId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(CorporationResponse.class);
    }

    /**
     * 军团联盟历史记录最大页数
     * de
     *
     * @param corporationId 军团ID
     * @param datasource    服务器
     * @return 最大页数
     *
     * @deprecated ESI接口header不存在X-Pages
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-军团联盟历史记录最大页数")
    @Deprecated
    public Integer queryCorporationAllianceHistoryMaxPage(Integer corporationId, String datasource) {
        String uri = "/corporations/" + corporationId + "/alliancehistory/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage("", uri,  apiClient);
    }

    /**
     * 军团联盟历史记录
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-军团联盟历史记录")
    public Flux<AllianceHistoryResponse> queryCorporationAllianceHistory(Integer corporationId, String datasource) {
        return apiClient.get().uri("/corporations/{corporation_id}/alliancehistory/?datasource={datasource}", corporationId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(AllianceHistoryResponse.class);
    }

    /**
     * 军团拥有的蓝图最大页数
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
    @Operation(summary = "ESI-军团拥有的蓝图最大页数")
    public Integer queryCorporationBlueprintsMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/blueprints/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 军团拥有的蓝图
     *
     * @param corporationId 军团ID
     * @param datasource     服务器数据源
     * @param page           页码
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团拥有的蓝图")
    public Flux<BlueprintResponse> queryCorporationBlueprints(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/blueprints/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(BlueprintResponse.class);
    }

    /**
     * 军团机库容器7天审计记录最大页数
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
    @Operation(summary = "ESI-军团机库容器7天审计记录最大页数")
    public Integer queryCorporationContainersLogsMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/containers/logs/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 军团机库容器7天审计记录
     *
     * @param corporationId 军团ID
     * @param datasource     服务器数据源
     * @param page           页码
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团机库容器7天审计记录")
    public Flux<ContainersLogsResponse> queryCorporationContainersLogs(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/containers/logs/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ContainersLogsResponse.class);
    }

    /**
     * 军团部门账户名称
     *
     * @param corporationId 军团ID
     * @param datasource     服务器数据源
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团部门账户名称")
    public Flux<DivisionNamesResponse> queryCorporationDivisions(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/divisions/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(DivisionNamesResponse.class);
    }

    /**
     * 军团设施
     *
     * @param corporationId 军团ID
     * @param datasource     服务器数据源
     * @param accessesToken  授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团设施")
    public Flux<CorporationFacilitiesResponse> queryCorporationFacilities(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/facilities/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(CorporationFacilitiesResponse.class);
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
    public Mono<IconResponse> queryCorporationIcons(Integer corporationId, String datasource) {
        return apiClient.get().uri("/corporations/{corporation_id}/icons/?datasource={datasource}", corporationId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(IconResponse.class);
    }

    /**
     * 军团勋章信息最大页数
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
    @Operation(summary = "ESI-军团勋章信息最大页数")
    public Integer queryCorporationMedalsMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/medals/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 军团勋章信息
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
    @Operation(summary = "ESI-军团勋章信息")
    public Flux<MedalResponse> queryCorporationMedals(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/medals/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MedalResponse.class);
    }

    /**
     * 军团发布的勋章信息最大页数
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
    @Operation(summary = "ESI-军团发布的勋章信息最大页数")
    public Integer queryCorporationIssuedMedalsMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/medals/issued/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
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
    public Flux<MedalResponse> queryCorporationIssuedMedals(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/medals/issued/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
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
    public Flux<Long> queryCorporationMembers(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/members/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Long.class);
    }

    /**
     * 军团成员上限
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
    @Operation(summary = "ESI-军团成员上限")
    public Mono<Integer> queryCorporationMembersLimit(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/members/limit/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Integer.class);
    }

    /**
     * 军团成员职位
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
    @Operation(summary = "ESI-军团成员职位")
    public Flux<MemberTitleResponse> queryCorporationMembersTitles(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/members/titles/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MemberTitleResponse.class);
    }

    /**
     * 军团成员追踪职位
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
    @Operation(summary = "ESI-军团成员追踪职位")
    public Flux<MemberTrackingResponse> queryCorporationMembersTracking(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/membertracking/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MemberTrackingResponse.class);
    }

    /**
     * 军团成员角色,  需要授权人物拥有人事权限或者总监权限
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
    @Operation(summary = "ESI-军团成员角色")
    public Flux<MemberRolesResponse> queryCorporationMembersRoles(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/roles/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(MemberRolesResponse.class);
    }

    /**
     * 军团成员角色变更记录
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
    @Operation(summary = "ESI-军团成员角色变更记录")
    public Flux<RoleChangeResponse> queryCorporationMembersRolesHistory(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/roles/history/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(RoleChangeResponse.class);
    }

    /**
     * 军团股东最大页数
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
    @Operation(summary = "ESI-军团股东最大页数")
    public Integer queryCorporationShareHoldersMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/shareholders/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 军团股东
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
    @Operation(summary = "ESI-军团股东")
    public Flux<ShareHolderResponse> queryCorporationShareHolders(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/shareholders/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ShareHolderResponse.class);
    }

    /**
     * 军团声望最大页数
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
    @Operation(summary = "ESI-军团声望最大页数")
    public Integer queryCorporationStandingMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/standings/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 军团声望
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
    @Operation(summary = "ESI-军团声望")
    public Flux<ShareHolderResponse> queryCorporationStanding(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/standings/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(ShareHolderResponse.class);
    }

    /**
     * 军团母星建筑（POS)最大页数
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
    @Operation(summary = "ESI-军团母星建筑（POS)最大页数")
    public Integer queryCorporationStarBasesMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/starbases/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 军团母星建筑（POS)
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
    @Operation(summary = "ESI-军团母星建筑（POS)")
    public Flux<StarBaseResponse> queryCorporationStarBases(Integer corporationId, String datasource, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/starbases/?datasource={datasource}&page={page}", corporationId, datasource, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(StarBaseResponse.class);
    }

    /**
     * 军团母星建筑（POS)配置信息
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @param systemId      星系ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "systemId", description = "星系ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团母星建筑（POS)配置信息")
    public Mono<StarBaseConfigResponse> queryCorporationStarBase(Integer corporationId, String datasource, Integer systemId, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/starbases/{starbase_id}/?datasource={datasource}&system_id={system_id}", corporationId, datasource, systemId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(StarBaseConfigResponse.class);
    }

    /**
     * 军团建筑信息最大页数
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
    @Operation(summary = "ESI-军团建筑信息最大页数")
    public Integer queryCorporationStructuresMaxPage(Integer corporationId, String datasource, String accessesToken) {
        String uri = "/corporations/" + corporationId + "/structures/?datasource=" + datasource + "&page=1";
        return pageTotalApi.queryMaxPage(accessesToken, uri,  apiClient);
    }

    /**
     * 军团建筑信息
     *
     * @param corporationId 军团ID
     * @param datasource    服务器数据源
     * @param language      语言
     * @param page          页码
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "corporationId", description = "军团ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
            @Parameter(name = "page", description = "页码", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-军团建筑信息")
    public Flux<StructuresInformationResponse> queryCorporationStructures(Integer corporationId, String datasource, String language, Integer page, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/structures/?datasource={datasource}&language={language}&page={page}", corporationId, datasource, language, page)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(StructuresInformationResponse.class);
    }

    /**
     * 军团职位信息
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
    @Operation(summary = "ESI-军团职位信息")
    public Flux<CorporationTitleResponse> queryCorporationTitles(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/titles/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(CorporationTitleResponse.class);
    }

    /**
     * NPC军团列表
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-NPC军团列表")
    public Flux<Integer> queryNpcCorporation(String datasource) {
        return apiClient.get().uri("/corporations/npccorps/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

}
