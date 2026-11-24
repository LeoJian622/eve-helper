package xyz.foolcat.eve.evehelper.domain.port.esi;

import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.IResultCode;

/**
 * ESI 访问异常(domain 层端口异常)。
 *
 * <p>表达 domain 层能理解的 ESI 访问失败语义,使 domain 不依赖 infrastructure 的
 * {@code EsiException} / {@code ResultCode}。继承 {@link EveHelperException},
 * 全局异常处理器可统一捕获并映射 HTTP 状态码。</p>
 *
 * <p>infrastructure 防腐层({@code EsiApiService})负责将 SDK 的 {@code EsiException}
 * 转换为本异常;domain 层 Service 也可直接抛出(如业务前置条件不满足)。</p>
 *
 * @author Leojan
 */
public class EsiAccessException extends EveHelperException {

    /**
     * ESI 数据接口权限不足(对应 HTTP 403 + ESI00403)。
     * <p>与人物缺少目标军团的 Director/Bookkeeper 等角色、已离开或不在目标军团相关。
     * 与授权失效(token 过期)不同:此处 token 有效,但角色无权访问目标数据。</p>
     */
    public static final String MSG_PERMISSION_LOW =
            "该人物缺少目标军团的 Director/Bookkeeper 等角色、已离开或不在目标军团，请确认角色或重新授权";

    /**
     * ESI 授权失效(对应 HTTP 401 + ESI00400)。
     * <p>refreshToken 失效或过期,需重新授权。</p>
     */
    public static final String MSG_AUTHORIZATION_FAILURE =
            "ESI 未授权或授权过期，请重新授权";

    /**
     * ESI 服务端错误(对应 HTTP 5xx + ESI00500)。
     */
    public static final String MSG_SERVER_FAILURE =
            "ESI 服务器请求失败";

    public EsiAccessException(String message) {
        super(message);
    }

    public EsiAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 携带结果码构造(IMP-1):message 取 {@link IResultCode#getMsg()},
     * resultCode 供全局异常处理器映射 HTTP 状态码与专属错误码。
     */
    public EsiAccessException(IResultCode resultCode) {
        super(resultCode);
    }

    /**
     * 工厂方法:权限不足(人物缺少军团角色)。
     * <p>携带 {@link Code#PERMISSION_LOW}(ESI00403 → HTTP 403)。</p>
     */
    public static EsiAccessException permissionLow() {
        return new EsiAccessException(Code.PERMISSION_LOW);
    }

    /**
     * 工厂方法:授权失效(token 过期)。
     * <p>携带 {@link Code#AUTHORIZATION_FAILURE}(ESI00400)。</p>
     */
    public static EsiAccessException authorizationFailure() {
        return new EsiAccessException(Code.AUTHORIZATION_FAILURE);
    }

    /**
     * 工厂方法:服务端错误(5xx)。
     * <p>携带 {@link Code#SERVER_FAILURE}(ESI00500)。</p>
     */
    public static EsiAccessException serverFailure() {
        return new EsiAccessException(Code.SERVER_FAILURE);
    }

    /**
     * ESI 访问结果码(domain 本地,实现共享层 {@link IResultCode})。
     * <p>
     * IMP-1:此前工厂走 {@code super(message)} 致 resultCode=null,全局异常处理器
     * 退化映射 HTTP 400(403+ESI00403 语义丢失)。码值与 infrastructure
     * {@code external/esi/ResultCode} 的 ESI 段逐字一致(跨层契约:
     * {@code GlobalExceptionHandler} / {@code WalletBalanceApplicationService}
     * 均按码字符串比对);domain 层不得 import infrastructure 枚举(DDD 分层),
     * 故在此本地定义。<b>码值变更须同步两处。</b></p>
     */
    public enum Code implements IResultCode {

        /** ESI 授权失效(refreshToken 失效或过期)。 */
        AUTHORIZATION_FAILURE("ESI00400", MSG_AUTHORIZATION_FAILURE),

        /** ESI 数据接口权限不足(HTTP 403)。 */
        PERMISSION_LOW("ESI00403", MSG_PERMISSION_LOW),

        /** ESI 服务端错误(5xx)。 */
        SERVER_FAILURE("ESI00500", MSG_SERVER_FAILURE);

        private final String code;
        private final String msg;

        Code(String code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMsg() {
            return msg;
        }
    }
}
