package xyz.foolcat.eve.evehelper.shared.kernel.constants;

/**
 * ??????????????????
 *
 * @author Leojan
 * date 2022-01-15 11:38
 */

public interface SecurityConstant {

    /**
     * ???????????????key
     */
    String AUTHORIZATION_KEY = "Authorization";

    /**
     * JWT????????????
     */
    String JWT_PREFIX = "Bearer ";

    /**
     * Basic????????????
     */
    String BASIC_PREFIX = "Basic ";

    /**
     * JWT??????key
     */
    String JWT_PAYLOAD_KEY = "payload";

    /**
     * JWT ID ????????????
     */
    String JWT_JTI = "jti";

    /**
     * JWT ID ????????????
     */
    String JWT_EXP = "exp";

    /**
     * ?????????token??????
     */
    String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";

    String USER_ID_KEY = "userId";

    String USER_NAME_KEY = "username";

    /**
     * JWT??????????????????
     */
    String AUTHORITY_PREFIX = "ROLE_";

    /**
     * JWT??????????????????
     */
    String JWT_AUTHORITIES_KEY = "authorities";

    String GRANT_TYPE_KEY = "grant_type";

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
