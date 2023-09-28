package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.common.result.ResultCode;
import xyz.foolcat.eve.evehelper.esi.model.AlliancesIconResponse;
import xyz.foolcat.eve.evehelper.esi.model.AlliancesResponse;
import xyz.foolcat.eve.evehelper.esi.model.AuthErrorResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * ESI 联盟相关接口
 *
 * @author Leojan
 * @date 2023-09-15 15:54
 */

@Service
@RequiredArgsConstructor
public class AlliancesApi {

    private final WebClient apiClient;

    /**
     * 获取全部活动联盟ID列表
     *
     * @return Flux<Long>
     */
    @Parameters({
            @Parameter(name = "datasource",description = "serenity" ,required = true),
    })
    @Operation(summary = "ESI-活跃联盟ID列表")
    public Flux<Long> queryAllActivePlayerAlliances(String datasource) {
        return apiClient.get().uri("/alliances/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Long.class);
    }

    /**
     * 根据ID查询联盟公开信息
     *
     * @param allianceId 联盟ID
     * @return Mono<AlliancesResponse>
     */
    @Parameters({
            @Parameter(name = "allianceId",description = "联盟ID" ,required = true),
            @Parameter(name = "datasource",description = "serenity" ,required = true),
    })
    @Operation(summary = "ESI-联盟公开信息")
    public Mono<AlliancesResponse> queryAlliancesPublicInformation(Long allianceId, String datasource) {
        return apiClient.get().uri("/alliances/{alliance_id}/?datasource={datasource}", allianceId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(AlliancesResponse.class);
    }

    /**
     * 查询联盟名下全部军团ID
     *
     * @param allianceId 联盟ID
     * @return Flux<Long>
     */
    @Parameters({
            @Parameter(name = "allianceId",description = "联盟ID" ,required = true),
            @Parameter(name = "datasource",description = "serenity" ,required = true),
    })
    @Operation(summary = "ESI-联盟名下全部军团ID")
    public Flux<Long> queryAlliancesCorporations(Long allianceId, String datasource) {
        return apiClient.get().uri("/alliances/{alliance_id}/corporations/?datasource={datasource}", allianceId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Long.class);
    }

    /**
     * 获取联盟图标地址
     *
     * @param allianceId 联盟ID
     * @return Mono<AlliancesIconResponse>
     */
    @Parameters({
            @Parameter(name = "allianceId",description = "联盟ID" ,required = true),
            @Parameter(name = "datasource",description = "serenity" ,required = true),
    })
    @Operation(summary = "ESI-联盟图标地址")
    public Mono<AlliancesIconResponse> queryAlliancesIcon(Long allianceId, String datasource) {
        return apiClient.get().uri("/alliances/{alliance_id}/icons/?datasource={datasource}", allianceId, datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(AuthErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILUE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(AlliancesIconResponse.class);
    }

}
