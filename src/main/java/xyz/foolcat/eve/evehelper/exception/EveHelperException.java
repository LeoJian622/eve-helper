package xyz.foolcat.eve.evehelper.exception;

import lombok.Getter;
import xyz.foolcat.eve.evehelper.common.result.IResultCode;

/**
 * @author Leojan
 * date 2021-12-20 17:05
 */

@Getter
public class EveHelperException  extends RuntimeException{

    public IResultCode resultCode;

    public EveHelperException(IResultCode errorCode) {
        super(errorCode.getMsg());
        this.resultCode = errorCode;
    }

    public EveHelperException(String message) {
        super(message);
    }

    public EveHelperException(String message, Throwable cause) {
        super(message,cause);
    }

    public EveHelperException(Throwable cause) {
        super(cause);
    }
}
