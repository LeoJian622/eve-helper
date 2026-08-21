package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiException;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiStatusUtil;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.ResultCode;

import java.time.Duration;
import java.util.Objects;

/**
 * 获取对应接口下数据总页数
 *
 * @author Leojan
 * date 2024-06-07 9:53
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Tag(name = "ESI 最大页码")
public class PageTotalApi {

    /**
     * HTTP headers 最大页数
     */
    private final String X_PAGES = "X-Pages";

    /**
     * ESI 数据接口调用超时(宪法:外部调用 5s 超时)
     */
    private static final Duration ESI_DATA_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 发送请求。
     *
     * <p>修复(013/T008):此前的 4xx/5xx 分支用 {@code .map().flatMap()} 而未 return,
     * 异常被丢弃后仍走 {@code toEntity} 把错误当 200 解析,导致 ESI 403/5xx 时
     * 对空 X-Pages 头 parseInt 抛 NPE/500。现改为正确 return {@code Mono.error},并:</p>
     * <ul>
     *     <li>403 → {@link EsiException}({@link ResultCode#ESI_AUTH_PERMISSION_LOW}),message 友好(M1/H2)</li>
     *     <li>其余 4xx → {@link ResultCode#ESI_AUTHORIZATION_FAILURE};5xx → {@link ResultCode#ESI_SERVER_FAILURE}</li>
     *     <li>响应链加 5s 超时(L1 宪法要求)</li>
     *     <li>去 assert(JVM 默认无 -ea 退化为空语句)与空头兜底</li>
     * </ul>
     *
     * @param accessesToken 授权Token
     * @param uri           请求的uri
     * @param esiClient     ESI WebClient
     * @return 最大页数
     */
    public int queryMaxPage(String accessesToken, String uri, WebClient esiClient) {
        ResponseEntity<String> responseEntity = esiClient.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> {
                    HttpStatusCode httpStatusCode = response.statusCode();
                    if (httpStatusCode.is4xxClientError()) {
                        if (EsiStatusUtil.isForbidden(httpStatusCode)) {
                            log.warn("ESI 军团/人物数据分页权限不足(403): uri={}", uri);
                            return Mono.error(EsiStatusUtil.forbiddenAgent());
                        }
                        return Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));
                    }
                    if (httpStatusCode.is5xxServerError()) {
                        log.warn("ESI 数据分页服务端错误(5xx): uri={}, status={}", uri, httpStatusCode.value());
                        return Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE));
                    }
                    return response.toEntity(String.class);
                }).timeout(ESI_DATA_TIMEOUT).block();
        if (responseEntity == null) {
            throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
        }
        String xPages = responseEntity.getHeaders().getFirst(X_PAGES);
        if (xPages == null) {
            log.warn("ESI 分页响应缺少 X-Pages 头: uri={}, status={}", uri,
                    responseEntity.getStatusCode().value());
            throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
        }
        return Integer.parseInt(xPages);
    }

    /**
     * 发送请求并同时返回最大页数与响应头(015/T008,TTL 来源通路)。
     *
     * <p>与 {@link #queryMaxPage} 取同一页数据,但在解析 X-Pages 之外**额外透出
     * {@code HttpHeaders}**(含 {@code Cache-Control: max-age})。原方法丢弃头、只返回 int,
     * 包裹层拿不到 maxPage 的缓存 TTL;本入口将 {@code maxPage + headers} 一并回传,
     * 供 T008 及后续 T009-T011 的 maxPage 缓存判读复用。</p>
     *
     * <p>4xx/5xx/缺 X-Pages 分支与 {@link #queryMaxPage} 完全一致;原方法保留零破坏。</p>
     *
     * @param accessesToken 授权Token
     * @param uri           请求的uri
     * @param esiClient     ESI WebClient
     * @return 最大页数与响应头的只读结果
     */
    public PageCountResult queryMaxPageWithHeaders(String accessesToken, String uri, WebClient esiClient) {
        ResponseEntity<String> responseEntity = esiClient.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, accessesToken)
                .exchangeToMono(response -> {
                    HttpStatusCode httpStatusCode = response.statusCode();
                    if (httpStatusCode.is4xxClientError()) {
                        if (EsiStatusUtil.isForbidden(httpStatusCode)) {
                            log.warn("ESI 军团/人物数据分页权限不足(403): uri={}", uri);
                            return Mono.error(EsiStatusUtil.forbiddenAgent());
                        }
                        return Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));
                    }
                    if (httpStatusCode.is5xxServerError()) {
                        log.warn("ESI 数据分页服务端错误(5xx): uri={}, status={}", uri, httpStatusCode.value());
                        return Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE));
                    }
                    return response.toEntity(String.class);
                }).timeout(ESI_DATA_TIMEOUT).block();
        if (responseEntity == null) {
            throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
        }
        HttpHeaders headers = responseEntity.getHeaders();
        String xPages = headers.getFirst(X_PAGES);
        if (xPages == null) {
            log.warn("ESI 分页响应缺少 X-Pages 头: uri={}, status={}", uri,
                    responseEntity.getStatusCode().value());
            throw new EsiException(ResultCode.ESI_SERVER_FAILURE);
        }
        return new PageCountResult(Integer.parseInt(xPages), headers);
    }

    /**
     * 分页读结果:最大页数与原始响应头(TTL 解析来源)。
     *
     * @param maxPage 最大页数(X-Pages 头解析值)
     * @param headers 原始响应头(含 Cache-Control/Expires)
     */
    public record PageCountResult(int maxPage, HttpHeaders headers) {
    }
}
