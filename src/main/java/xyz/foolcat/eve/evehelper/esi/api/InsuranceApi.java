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
import xyz.foolcat.eve.evehelper.esi.model.InsuranceOfShipResponse;
import xyz.foolcat.eve.evehelper.exception.EsiException;

/**
 * 舰船保险
 *
 * @author Leojan
 * @date 2023-10-31 11:39
 */

@Service
@RequiredArgsConstructor
@Tag(name = "ESI 舰船保险")
public class InsuranceApi {

    private final WebClient apiClient;

    /**
     * 舰船保险
     *
     * @param datasource 服务器
     * @return
     */
    @Parameters({
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
    })
    @Operation(summary = "ESI-舰船保险")
    public Flux<InsuranceOfShipResponse> queryIndustrySystems(String datasource, String language) {
        return apiClient.get().uri("/insurance/prices/?datasource={datasource}&language={language}", datasource, language)
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription()))))
                .bodyToFlux(InsuranceOfShipResponse.class);
    }
}
