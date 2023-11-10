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
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.esi.model.OpportunitiesGroupResponse;
import xyz.foolcat.eve.evehelper.esi.model.OpportunitiesResponse;
import xyz.foolcat.eve.evehelper.esi.model.OpportunitiesTaskResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 机遇系统接口
 *
 * @author Leojan
 * @date 2023-11-07 9:57
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 机遇系统接口")
public class OpportunitiesApi {

    private final WebClient apiClient;

    /**
     * 查询人物已完成的机遇任务
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
    @Operation(summary = "ESI-查询人物订单")
    public Flux<OpportunitiesResponse> queryCharacterOpportunities(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/opportunities/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(OpportunitiesResponse.class);
    }

    /**
     * 查询机遇任务分组ID
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-查询机遇任务分组ID")
    public Flux<Integer> queryOpportunitiesGroups(String datasource) {
        return apiClient.get().uri("/opportunities/groups/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 查询机遇任务组信息
     *
     * @param groupId    机遇任务组ID
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "groupId", description = "机遇任务组ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-查询机遇任务组信息")
    public Mono<OpportunitiesGroupResponse> queryOpportunitiesGroupsDetails(Integer groupId, String datasource, String language) {
        return apiClient.get().uri("/opportunities/groups/{group_id}/?datasource={datasource}&language={language}", groupId, datasource, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(OpportunitiesGroupResponse.class);
    }

    /**
     * 查询机遇任务ID
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-查询人机遇任务ID")
    public Flux<Integer> queryOpportunitiesTasks(String datasource) {
        return apiClient.get().uri("/opportunities/tasks/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 查询机遇任务信息
     *
     * @param taskId    机遇任务ID
     * @param datasource 服务器数据源
     * @param language   语言
     * @return
     */
    @Parameters({
            @Parameter(name = "taskId", description = "机遇任务ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "language", description = "语言", required = true),
    })
    @Operation(summary = "ESI-查询机遇任务信息")
    public Mono<OpportunitiesTaskResponse> queryOpportunitiesTaskDetails(Integer taskId, String datasource, String language) {
        return apiClient.get().uri("/opportunities/tasks/{task_id}/?datasource={datasource}&language={language}", taskId, datasource, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(OpportunitiesTaskResponse.class);
    }

}
