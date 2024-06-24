package xyz.foolcat.eve.evehelper.esi.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * HTTP headers 最大页数
     */
    private final String X_PAGES = "X-Pages";


    /**
     * 发送请求
     *
     * @param accessesToken 授权Token
     * @param uri           请求的uri
     * @return 最大页数
     */
    public int queryMaxPage(String accessesToken, String uri, WebClient apiClient) {
        ResponseEntity<String> responseEntity = apiClient.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> {
                    HttpStatus httpStatus = response.statusCode();
                    if (httpStatus.is4xxClientError()) {
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE, res.getError() + ":" + res.getErrorDescription())));
                    }
                    if (httpStatus.is5xxServerError()) {
                        response.bodyToMono(ErrorResponse.class).flatMap(res -> Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE, res.getError() + ":" + res.getErrorDescription())));
                    }
                    return response.toEntity(String.class);
                }).block();
        assert responseEntity != null;
        return Integer.parseInt(Objects.requireNonNull(responseEntity.getHeaders().getFirst(X_PAGES)));
    }

}
