package xyz.foolcat.eve.evehelper.shared.kernel.constants;

/**
 * 安全相关常量(JWT 认证、RBAC 授权、token 黑名单等)。
 *
 * @author Leojan
 * date 2022-01-15 11:38
 */

public interface SecurityConstant {

    /**
     * HTTP Authorization 请求头 key
     */
    String AUTHORIZATION_KEY = "Authorization";

    /**
     * JWT 令牌前缀
     */
    String JWT_PREFIX = "Bearer ";

    /**
     * 登出 token 黑名单 key 前缀(配合 SESSION_ID_KEY 完成会话撤销)
     */
    String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";

    String USER_ID_KEY = "userId";

    String USER_NAME_KEY = "username";

    /**
     * JWT 载荷中的用户权限声明 key
     */
    String JWT_AUTHORITIES_KEY = "authorities";

    /**
     * 会话标识声明(007 T048)。
     * <p>登录时生成一次,<b>refresh token 轮换时原样继承</b> —— 与每次轮换都变的
     * {@code jti} 不同,它在整个会话生命周期内恒定,故可作为
     * 「access token → 当前 refresh token」索引的稳定锚点,使登出能撤销
     * <b>轮换之后</b>的 refresh token。
     */
    String SESSION_ID_KEY = "sid";

    String REFRESH_TOKEN = "refresh_token";

    /**
     * http method
     */

    String OPTIONS = "OPTIONS";

}
