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
 * ESI 势力战争接口
 *
 * @author Leojan
 * date 2023-10-26 16:18
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 属性接口")
public class FactionWarfareApi {

    private final WebClient apiClient;

    /**
     * 人物势力战争统计
     *
     * @param characterId   人物ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权码
     * @return
     */
    @Parameters({
            @Parameter(name = "characterId", description = "人物ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),

    })
    @Operation(summary = "ESI-人物势力战争统计")
    public Mono<FactionWarfareStatisticsResponse> queryCharacterFactionWarfareStats(Integer characterId, String datasource, String accessesToken) {
        return apiClient.get().uri("/characters/{character_id}/fw/stats/?datasource={datasource}", characterId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(FactionWarfareStatisticsResponse.class);
    }

    /**
     * 军团势力战争统计
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
    @Operation(summary = "ESI-军团势力战争统计")
    public Mono<FactionWarfareStatisticsResponse> queryCorporationFactionWarfareStats(Integer corporationId, String datasource, String accessesToken) {
        return apiClient.get().uri("/corporations/{corporation_id}/fw/stats/?datasource={datasource}", corporationId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(FactionWarfareStatisticsResponse.class);
    }

    /**
     * 势力战争排行榜
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),

    })
    @Operation(summary = "ESI-势力战争排行榜")
    public Mono<LeaderboardResponse> queryFactionWarfareLeaderboards(String datasource) {
        return apiClient.get().uri("/fw/leaderboards/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(LeaderboardResponse.class);
    }

    /**
     * 人物势力战争排行榜
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),

    })
    @Operation(summary = "ESI-人物势力战争排行榜")
    public Mono<LeaderboardCharacterResponse> queryFactionWarfareCharacterLeaderboards(String datasource) {
        return apiClient.get().uri("/fw/leaderboards/characters/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(LeaderboardCharacterResponse.class);
    }

    /**
     * 军团势力战争排行榜
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),

    })
    @Operation(summary = "ESI-军团势力战争排行榜")
    public Mono<LeaderboardCorporationResponse> queryFactionWarfareCorporationLeaderboards(String datasource) {
        return apiClient.get().uri("/fw/leaderboards/corporations/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToMono(LeaderboardCorporationResponse.class);
    }

    /**
     * 势力战争统计
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),

    })
    @Operation(summary = "ESI-势力战争统计")
    public Flux<FactionWarfareStatisticsResponse> queryFactionWarfareStats(String datasource) {
        return apiClient.get().uri("/fw/stats/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(FactionWarfareStatisticsResponse.class);
    }

    /**
     * 势力战争星系所有权信息
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),

    })
    @Operation(summary = "ESI-势力战争星系所有权信息")
    public Flux<FactionWarfareSolarSystemsResponse> queryFactionWarfareSystems(String datasource) {
        return apiClient.get().uri("/fw/systems/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(FactionWarfareSolarSystemsResponse.class);
    }


    /**
     * 战争中的NPC势力
     *
     * @param datasource 服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),

    })
    @Operation(summary = "ESI-战争中的NPC势力")
    public Flux<NpcFactionsAtWarResponse> queryFactionWarfareWars(String datasource) {
        return apiClient.get().uri("/fw/wars/?datasource={datasource}", datasource)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(NpcFactionsAtWarResponse.class);
    }

}
