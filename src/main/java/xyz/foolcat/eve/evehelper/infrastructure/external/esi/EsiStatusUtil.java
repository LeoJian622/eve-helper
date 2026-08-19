package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * ESI 状态识别与错误构造工具。
 *
 * <p>静态无状态,置于 ESI 防腐层。供各 {@code *.Api} 封装类的状态分支统一判定与构造
 * 异常,避免每处重复。核心语义(H1/H2/M1 设计决策):</p>
 * <ul>
 *     <li>{@code isForbidden} 按 HTTP 403 判定,不被 ESI 错误体内容劫持(M1:先判状态再解 body)</li>
 *     <li>{@code dataError} 供 {@code .onStatus(...)} 使用,统一把 4xx/5xx 折叠为对应
 *         {@link EsiException},message 一律用枚举友好文案,<b>不含 ESI 原始 error/errorDescription</b>
 *         (H2:安全红线,不泄露外部错误细节)</li>
 * </ul>
 */
@Slf4j
public final class EsiStatusUtil {

    private EsiStatusUtil() {
        // 静态工具,禁止实例化
    }

    /**
     * 判定是否为 ESI 数据接口权限不足(HTTP 403)。
     */
    public static boolean isForbidden(HttpStatusCode statusCode) {
        return statusCode != null && statusCode.value() == 403;
    }

    /**
     * 构造 ESI 权限不足异常。
     *
     * <p>message 用枚举 {@link ResultCode#ESI_AUTH_PERMISSION_LOW} 的友好文案。
     * 调用方如需 ESI 原始错误详情,应在日志层结构化输出,<b>不得</b>回显给前端。</p>
     */
    public static EsiException forbiddenAgent() {
        return new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW);
    }

    /**
     * 供 {@code .onStatus(HttpStatusCode::is4xxClientError/is5xxServerError, EsiStatusUtil::dataError)}
     * 使用的错误折叠函数。
     *
     * <p>统一映射:</p>
     * <ul>
     *     <li>HTTP 403 → {@link ResultCode#ESI_AUTH_PERMISSION_LOW}(数据权限不足)</li>
     *     <li>其余 4xx → {@link ResultCode#ESI_AUTHORIZATION_FAILURE}(授权失效/其它客户端错误)</li>
     *     <li>5xx → {@link ResultCode#ESI_SERVER_FAILURE}</li>
     * </ul>
     * <p>全程不回显 ESI 原始 error/errorDescription(H2),原始信息仅日志记录。</p>
     */
    public static Function<ClientResponse, Mono<? extends Throwable>> dataError() {
        return response -> {
            HttpStatusCode status = response.statusCode();
            if (isForbidden(status)) {
                log.warn("ESI 数据接口权限不足(HTTP 403): status={}", status.value());
                return Mono.error(forbiddenAgent());
            }
            ResultCode code = status.is5xxServerError()
                    ? ResultCode.ESI_SERVER_FAILURE
                    : ResultCode.ESI_AUTHORIZATION_FAILURE;
            log.warn("ESI 数据接口错误: status={}, code={}", status.value(), code.getMsg());
            return Mono.error(new EsiException(code));
        };
    }
}