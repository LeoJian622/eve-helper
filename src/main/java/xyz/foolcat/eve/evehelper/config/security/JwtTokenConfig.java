package xyz.foolcat.eve.evehelper.config.security;

import lombok.Data;

/**
 * @author Leojan
 * @date 2022-01-19 15:45
 */


@Data
public class JwtTokenConfig {

    private String subject;

    private String issuer;

    private long expirationTime;
}
