package xyz.foolcat.eve.evehelper.common.result;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Leojan
 * @date 2021-08-10 17:27
 */

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private String code;

    private T data;

    private String msg;

    private Integer total;

    public static <T> R<T> success() {
        return success(null);
    }

    public static <T> R<T> success(T data) {
        ResultCode rce = ResultCode.SUCCESS;
        if (data instanceof Boolean && Boolean.FALSE.equals(data)) {
            rce = ResultCode.SYSTEM_EXECUTION_ERROR;
        }
        return R(rce, data);
    }


    public static <T> R<T> success(T data, Long total) {
        R<T> R = new R();
        R.setCode(ResultCode.SUCCESS.getCode());
        R.setMsg(ResultCode.SUCCESS.getMsg());
        R.setData(data);
        R.setTotal(total.intValue());
        return R;
    }

    public static <T> R<T> failed() {
        return result(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), ResultCode.SYSTEM_EXECUTION_ERROR.getMsg(), null);
    }

    public static <T> R<T> failed(String msg) {
        return result(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), msg, null);
    }

    public static <T> R<T> judge(boolean status) {
        if (status) {
            return success();
        } else {
            return failed();
        }
    }

    public static <T> R<T> failed(IResultCode resultCode) {
        return result(resultCode.getCode(), resultCode.getMsg(), null);
    }

    private static <T> R<T> R(IResultCode resultCode, T data) {
        return result(resultCode.getCode(), resultCode.getMsg(), data);
    }

    public static <T> R<T> failed(IResultCode resultCode, String msg) {
        return result(resultCode.getCode(), msg, null);
    }

    private static <T> R<T> result(IResultCode resultCode, T data) {
        return result(resultCode.getCode(), resultCode.getMsg(), data);
    }

    private static <T> R<T> result(String code, String msg, T data) {
        R<T> result = new R<>();
        result.setCode(code);
        result.setData(data);
        result.setMsg(msg);
        return result;
    }


    public static boolean isSuccess(R R) {
        if(R!=null&& ResultCode.SUCCESS.getCode().equals(R.getCode())){
            return true;
        }
        return false;
    }
}
