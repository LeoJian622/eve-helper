package xyz.foolcat.eve.evehelper.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Token配置属性
 *
 * @author Leojan
 * date 2022-01-19 15:45
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt-token")
public class JwtTokenProperties {

    private String subject;

    private String issuer;

    /**
     * 旧版token过期时间(秒) - 保留兼容性
     */
    private long expirationTime;

    /**
     * Access Token过期时间(秒) - 默认15分钟
     */
    private long accessTokenExpirationTime = 900;

    /**
     * Refresh Token过期时间(秒) - 默认7天
     */
    private long refreshTokenExpirationTime = 604800;
}
