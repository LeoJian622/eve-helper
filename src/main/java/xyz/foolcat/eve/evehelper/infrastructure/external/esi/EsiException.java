package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import lombok.Getter;

/**
 * Esi请求错误异常
 *
 * @author Leojan
 * date 2021-12-08 16:35
 */

@Getter
public class EsiException extends RuntimeException{

    public ResultCode resultCode;

    public EsiException(ResultCode errorCode) {
        super(errorCode.getMsg());
        this.resultCode = errorCode;
    }

    public EsiException(ResultCode errorCode, String message) {
        super(message);
        this.resultCode = errorCode;
    }

    public EsiException(String message, Throwable cause) {
        super(message,cause);
    }

    public EsiException(Throwable cause) {
        super(cause);
    }
}
