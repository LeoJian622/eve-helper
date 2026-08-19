package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import org.springframework.http.HttpStatusCode;

/**
 * ESI 状态识别工具。
 *
 * <p>静态无状态,置于 ESI 防腐层。供各 {@code *.Api} 封装类的状态分支统一判定与构造
 * 403 异常,避免每处重复。核心语义(H1/H2/M1 设计决策):</p>
 * <ul>
 *     <li>{@code isForbidden} 按 HTTP 403 判定,不被 ESI 错误体内容劫持(M1:先判状态再解 body)</li>
 *     <li>{@code forbiddenAgent} 返回 {@link EsiException}({@link ResultCode#ESI_AUTH_PERMISSION_LOW}),
 *         message 用枚举友好文案,<b>不含 ESI 原始 error/errorDescription</b>(H2:安全红线,不泄露外部错误细节)</li>
 * </ul>
 */
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
}