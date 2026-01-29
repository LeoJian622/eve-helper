package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Leojan
 * date 2021-08-10 17:23
 */
@AllArgsConstructor
@NoArgsConstructor
public enum ResultCode implements Serializable {

    /**
     * 成功
     */
    SUCCESS("200", "成功"),
    /**
     * 失败
     */
    COMMON_FAIL("500","失败"),

    /**
     * 系统代码
     */
    UNKNOWN_ERROR("UNKNOWN", "未知异常"),

    /**
     * ESI登录异常
     */
    ESI_AUTHORIZATION_FAILURE("ESI00400","ESI 未授权或授权过期，请重新授权"),
    ESI_SERVER_FAILURE("ESI00500","ESI 服务器请求失败"),
    ESI_AUTH_TOKEN_NULL("ESI00400", "ESI author Token为空");

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    String code;

    String msg;

    @Override
    public String toString() {
        return "{" +
                "\"code\":\"" + code + '\"' +
                ", \"msg\":\"" + msg + '\"' +
                '}';
    }


    public static ResultCode getValue(String code){
        for (ResultCode value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        // 默认系统执行错误
        return UNKNOWN_ERROR;
    }
}
