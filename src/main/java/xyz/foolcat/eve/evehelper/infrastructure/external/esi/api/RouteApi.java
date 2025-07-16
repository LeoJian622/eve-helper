package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ErrorResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import java.util.List;

/**
 * ESI 路径接口
 *
 * @author Leojan
 * date 2023-11-07 16:08
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 机遇系统接口")
public class RouteApi {

    private final WebClient apiClient;

    /**
     * 路径导航规划
     *
     * @param origin      起始星系
     * @param destination 终点星系
     * @param avoid       规避的星系
     * @param connects    星系链接组   （途经点？）
     * @param datasource  服务器数据源
     * @return
     */
    @Parameters({
            @Parameter(name = "origin", description = "起始星系", required = true),
            @Parameter(name = "destination", description = "终点星系", required = true),
            @Parameter(name = "avoid", description = "规避的星系", required = true),
            @Parameter(name = "connects", description = "星系链接组   （途经点？）", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-路径导航规划")
    public Flux<Integer> queryUniverseSchematic(Integer origin, Integer destination, List<Integer> avoid, List<List<Integer>> connects, String datasource) {
        return apiClient.get().uri("/route/{origin}/{destination}/?datasource={datasource}&avoid={avoid}&connects={connects}", origin, destination, datasource, avoid, connects)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(Integer.class);
    }
}
