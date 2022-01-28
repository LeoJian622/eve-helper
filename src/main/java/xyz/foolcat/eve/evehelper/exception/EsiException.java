package xyz.foolcat.eve.evehelper.exception;

import com.dtflys.forest.annotation.Get;
import lombok.Data;
import lombok.Getter;
import xyz.foolcat.eve.evehelper.common.result.IResultCode;

import java.security.PrivilegedActionException;

/**
 * Esi请求错误异常
 *
 * @author Leojan
 * @date 2021-12-08 16:35
 */

@Getter
public class EsiException extends RuntimeException{

    public IResultCode resultCode;

    public EsiException(IResultCode errorCode) {
        super(errorCode.getMsg());
        this.resultCode = errorCode;
    }

    public EsiException(String message) {
        super(message);
    }

    public EsiException(String message, Throwable cause) {
        super(message,cause);
    }

    public EsiException(Throwable cause) {
        super(cause);
    }
}
