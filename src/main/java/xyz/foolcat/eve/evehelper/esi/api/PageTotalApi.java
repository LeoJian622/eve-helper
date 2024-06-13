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
import xyz.foolcat.eve.evehelper.exception.EsiException;

import java.util.Objects;

/**
 * 获取对应接口下数据总页数
 *
 * @author Leojan
 * date 2024-06-07 9:53
 */
@Service
@RequiredArgsConstructor
@Tag(name = "ESI 最大页码")
public class PageTotalApi {

    private final WebClient apiClient;

    /**
     * HTTP headers 最大页数
     */
    private final String X_PAGES = "X-Pages";

    /**
     * 联盟联系人
     *
     * @param allianceId    联盟ID
     * @param datasource    服务器数据源
     * @param accessesToken 授权Token
     * @return 最大页数
     */
    @Parameters({
            @Parameter(name = "allianceId", description = "联盟ID", required = true),
            @Parameter(name = "datasource", description = "服务器数据源", required = true),
            @Parameter(name = "accessesToken", description = "授权Token", required = true),
    })
    @Operation(summary = "ESI-联盟联系人最大页数")
    public Mono<Integer> queryAlliancesContactsPage(Long allianceId, String datasource, String accessesToken) {
        return apiClient.get().uri("/alliances/{alliance_id}/contacts/?datasource={datasource}&page=1", allianceId, datasource)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> {
                    HttpStatus httpStatus = response.statusCode();
                    if (httpStatus.is4xxClientError()) {
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription())));
                    }
                    if (httpStatus.is5xxServerError()) {
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription())));
                    }
                    return response.bodyToMono(Integer.class).flatMap(res -> Mono.justOrEmpty(Integer.parseInt(Objects.requireNonNull(response.headers().asHttpHeaders().getFirst(X_PAGES)))));
                });
    }

}
