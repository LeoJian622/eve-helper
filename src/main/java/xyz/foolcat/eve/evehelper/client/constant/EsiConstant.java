package xyz.foolcat.eve.evehelper.client.constant;

/**
 * @author Leojan
 * @date 2022-03-29 16:47
 */

public interface EsiConstant {

    /**
     * 客户端ID
     */
    String CLIENT_ID = "bc90aa496a404724a93f41b4f4e97761";

    /**
     * ESI返回错误类型
     */
    String INVALID_GRANT = "invalid_grant";

    /**
     * 认证方式
     */
   String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
   String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    /**
     * 返回参数
     */

    String ERROR = "error";
}
