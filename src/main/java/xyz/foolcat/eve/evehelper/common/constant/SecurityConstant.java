package xyz.foolcat.eve.evehelper.common.constant;

/**
 * 安全认证权限
 *
 * @author Leojan
 * @date 2022-01-15 11:38
 */

public interface SecurityConstant {

    /**
     * 认证请求头key
     */
    String AUTHORIZATION_KEY = "Authorization";

    /**
     * JWT令牌前缀
     */
    String JWT_PREFIX = "Bearer ";

    /**
     * Basic认证前缀
     */
    String BASIC_PREFIX = "Basic ";

    /**
     * JWT载体key
     */
    String JWT_PAYLOAD_KEY = "payload";

    /**
     * JWT ID 唯一标识
     */
    String JWT_JTI = "jti";

    /**
     * JWT ID 唯一标识
     */
    String JWT_EXP = "exp";

    /**
     * 黑名单token前缀
     */
    String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";

    String USER_ID_KEY = "userId";

    String USER_NAME_KEY = "username";

    /**
     * JWT存储权限前缀
     */
    String AUTHORITY_PREFIX = "ROLE_";

    /**
     * JWT存储权限属性
     */
    String JWT_AUTHORITIES_KEY = "authorities";

    String GRANT_TYPE_KEY = "grant_type";

    String REFRESH_TOKEN = "refresh_token";

    /**
     * http method
     */

    String OPTIONS = "OPTIONS";

}
