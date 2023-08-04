package xyz.foolcat.eve.evehelper.common.result;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Leojan
 * @date 2021-08-10 17:23
 */
@AllArgsConstructor
@NoArgsConstructor
public enum ResultCode implements IResultCode, Serializable {

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
    SYSTEM_EXECUTION_ERROR("SYS00101", "系统执行出错"),

    /**
     * 认证授权异常
     */
    TOKEN_INVALID_OR_EXPIRED("AUT00201", "用户未登录"),
    TOKEN_ACCESS_FORBIDDEN("AUT00202", "TOKEN被禁用"),
    USER_ACCOUNT_EXPIRED("AUT00203", "账号过期"),
    USER_CREDENTIALS_ERROR("AUT00204", "密码错误"),
    USER_USER_CREDENTIALS_EXPIRED("AUT00205", "密码过期"),
    USER_ACCOUNT_DISABLE("AUT00206", "账号不可用"),
    USER_ACCOUNT_LOCKED("AUT00207", "账户被锁定"),
    USER_ACCOUNT_NOT_EXIST("AUT00208", "账号不存在"),
    USER_ACCOUNT_ALREADY_EXIST("AUT00209", "账号已存在"),
    TOKEN_ACCESS_EXPIRED("AUT00210", "TOKEN过期"),
    TOKEN_VERIFY_FAILED("AUT00211", "非法TOKEN"),

    ACCESS_UNAUTHORIZED("AUT00301", "访问未授权"),

    PARAM_ERROR("SYS00400", "用户请求参数错误"),
    RESOURCE_NOT_FOUND("SYS00401", "请求资源不存在"),
    PARAM_IS_NULL("SYS00410", "请求必填参数为空"),

    /**
     * ESI登录异常
     */
    ESI_AUTHORIZATION_FAILUE("ESI00403","ESI 未授权或授权过期，请重新登录"),

    ;

    @Override
    public String getCode() {
        return code;
    }

    @Override
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
        return SYSTEM_EXECUTION_ERROR;
    }
}
