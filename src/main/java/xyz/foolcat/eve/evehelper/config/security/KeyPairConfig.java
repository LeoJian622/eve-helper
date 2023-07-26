package xyz.foolcat.eve.evehelper.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.security.KeyPair;

/**
 * @author Leojan
 * @date 2022-01-19 17:05
 */

@Configuration
public class KeyPairConfig {

    /**
     * 密钥库中获取密钥对(公钥+私钥)
     */
    @Bean
    public KeyPair keyPair() {
        KeyStoreKeyFactory factory = new KeyStoreKeyFactory(new ClassPathResource("eve-jwt.jks"), "eve000".toCharArray());
        return factory.getKeyPair("eve-jwt", "eve000".toCharArray());
    }

}
