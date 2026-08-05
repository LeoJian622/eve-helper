package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

/**
 * Esi请求错误异常
 * <p>
 * 继承共享层业务异常 {@link EveHelperException},使接口层无需依赖
 * infrastructure 即可通过统一业务异常处理器捕获并转译为 Result。
 *
 * @author Leojan
 * date 2021-12-08 16:35
 */

public class EsiException extends EveHelperException {

    public EsiException(ResultCode errorCode) {
        super(errorCode);
    }

    public EsiException(ResultCode errorCode, String message) {
        super(message);
        this.resultCode = errorCode;
    }

    public EsiException(String message, Throwable cause) {
        super(message, cause);
    }

    public EsiException(Throwable cause) {
        super(cause);
    }
}
