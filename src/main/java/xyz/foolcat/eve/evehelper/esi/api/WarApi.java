package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.esi.model.KillMailsIdAndHashResponse;
import xyz.foolcat.eve.evehelper.esi.model.WarDetailsResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 战争接口
 *
 * @author Leojan
 * date 2023-11-14 14:07
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 战争接口")
public class WarApi {

    private final WebClient apiClient;

    /**
     * 战争ID清单
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-战争ID清单")
    public Flux<Integer> queryWars(String datasource) {
        return apiClient.get().uri("/wars/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }

    /**
     * 战争详细信息
     *
     * @param warId 战争ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "warId", description = "战争ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-战争详细信息")
    public Mono<WarDetailsResponse> queryWarDetails(Integer warId, String datasource) {
        return apiClient.get().uri("/wars/{war_id}/?datasource={datasource}", warId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(WarDetailsResponse.class);
    }

    /**
     * 战争击毁报告
     *
     * @param warId 战争ID
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "warId", description = "战争ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-战争击毁报告")
    public Flux<KillMailsIdAndHashResponse> queryWarsKillMails(Integer warId, String datasource) {
        return apiClient.get().uri("/wars/{war_id}/killmails/?datasource={datasource}", warId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(KillMailsIdAndHashResponse.class);
    }

}
