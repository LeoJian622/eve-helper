package xyz.foolcat.eve.evehelper.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Leojan
 * @date 2022-01-19 15:45
 */


@Data
@Configuration
@ConfigurationProperties(prefix = "jwt-token")
public class JwtTokenConfig {

    private String subject;

    private String issuer;

    private long expirationTime;
}
