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
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.esi.model.send.NewMailUI;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 用户界面操作接口
 *
 * @author Leojan
 * date 2023-11-19 20:03
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 用户界面操作接口")
public class UserInterfaceApi {

    private final WebClient apiClient;

    /**
     * 添加自动导航航标
     *
     * @param datasource          服务器数据源
     * @param addToBeginning      是否是起始点
     * @param clearOtherWaypoints 是否清除其他航标
     * @param destinationId       目标点ID
     * @param accessesToken       授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "addToBeginning", description = "是否是起始点", required = true),
            @Parameter(name = "clearOtherWaypoints", description = "是否清除其他航标", required = true),
            @Parameter(name = "destinationId", description = "目标点ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-添加自动导航航标")
    public Mono<Object> addWaypoint(String datasource, Boolean addToBeginning, Boolean clearOtherWaypoints, Long destinationId, String accessesToken) {
        return apiClient.post().uri("/ui/autopilot/waypoint/?datasource={datasource}&add_to_beginning={addToBeginning}&clear_other_waypoints={clearOtherWaypoints}&destination_id={destinationId}",
                        datasource, addToBeginning, clearOtherWaypoints, destinationId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 在客户端内打开合同窗口
     *
     * @param datasource    服务器数据源
     * @param contractId    合同ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "contractId", description = "合同ID", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-在游戏中打开对应的合同")
    public Mono<Object> openContract(String datasource, Integer contractId, String accessesToken) {
        return apiClient.post().uri("/ui/openwindow/contract/?datasource={datasource}&contract_id={contractId}",
                        datasource, contractId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 打开客户端内角色、公司或联盟的信息窗口
     *
     * @param datasource    服务器数据源
     * @param targetId      角色、公司或联盟的ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "targetId", description = "角色、公司或联盟的ID ", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-打开客户端内角色、公司或联盟的信息窗口")
    public Mono<Object> openInformation(String datasource, Integer targetId, String accessesToken) {
        return apiClient.post().uri("/ui/openwindow/information/?datasource={datasource}&target_id={targetId}",
                        datasource, targetId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 在客户端中打开特定 typeID 的市场详细信息窗口
     *
     * @param datasource    服务器数据源
     * @param typeId        物品类型ID
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "typeId", description = "物品类型ID ", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-在客户端中打开特定 typeID 的市场详细信息窗口")
    public Mono<Object> openMarketDetails(String datasource, Integer typeId, String accessesToken) {
        return apiClient.post().uri("/ui/openwindow/marketdetails/?datasource={datasource}&type_id={typeId}",
                        datasource, typeId)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }

    /**
     * 根据请求中的设置（如果适用）打开“新建邮件”窗口
     *
     * @param datasource    服务器数据源
     * @param newMail       新建邮件的设置
     * @param accessesToken 授权Token
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "newMail", description = "新建邮件的设置 ", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-根据请求中的设置（如果适用）打开“新建邮件”窗口")
    public Mono<Object> openNewMail(String datasource, NewMailUI newMail, String accessesToken) {
        return apiClient.post().uri("/ui/openwindow/newmail/?datasource={datasource}",
                        datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .body(Mono.just(newMail), NewMailUI.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(Object.class);
    }
}
