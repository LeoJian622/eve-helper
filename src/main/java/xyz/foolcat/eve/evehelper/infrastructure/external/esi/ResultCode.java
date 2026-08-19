package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import xyz.foolcat.eve.evehelper.shared.result.IResultCode;

import java.io.Serializable;

/**
 * @author Leojan
 * date 2021-08-10 17:23
 */
@AllArgsConstructor
@NoArgsConstructor
public enum ResultCode implements IResultCode, Serializable {

    /**
     * 成功
     */
    SUCCESS("200", "成功"),

    /**
     * ESI登录异常
     */
    ESI_AUTHORIZATION_FAILURE("ESI00400","ESI 未授权或授权过期，请重新授权"),
    ESI_SERVER_FAILURE("ESI00500","ESI 服务器请求失败"),
    /**
     * ESI 数据接口权限不足(HTTP 403)
     * <p>与该人物缺少目标军团的 Director/Bookkeeper 等角色、已离开或不在目标军团相关。
     * 与 {@link #ESI_AUTHORIZATION_FAILURE}(refreshToken 失效)不同:此处 token 有效,但该角色
     * 无权访问目标军团/联盟数据。message 友好可行动,实现层不得改造成 ESI 原始 error 串(安全红线)。</p>
     */
    ESI_AUTH_PERMISSION_LOW("ESI00403","该人物缺少目标军团的 Director/Bookkeeper 等角色、已离开或不在目标军团，请确认角色或重新授权");

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
        return ESI_SERVER_FAILURE;
    }
}
