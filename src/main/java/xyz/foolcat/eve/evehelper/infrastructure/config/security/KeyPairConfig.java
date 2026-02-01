package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.security.KeyPair;

/**
 * 密钥对配置
 * 从环境变量加载密钥库密码,避免硬编码
 *
 * @author Leojan
 * date 2022-01-19 17:05
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KeyPairConfig {

    private final SecurityProperties securityProperties;

    /**
     * 密钥库中获取密钥对(公钥+私钥)
     */
    @Bean
    public KeyPair keyPair() {
        SecurityProperties.Keystore keystoreConfig = securityProperties.getKeystore();

        // 验证必要的配置项
        if (!StringUtils.hasText(keystoreConfig.getPassword())) {
            throw new IllegalStateException("密钥库密码未配置,请设置环境变量 KEYSTORE_PASSWORD");
        }
        if (!StringUtils.hasText(keystoreConfig.getKeyPassword())) {
            throw new IllegalStateException("密钥密码未配置,请设置环境变量 KEY_PASSWORD");
        }

        log.info("正在从密钥库加载密钥对: {}", keystoreConfig.getLocation());

        KeyStoreKeyFactory factory = new KeyStoreKeyFactory(
                new ClassPathResource(keystoreConfig.getLocation()),
                keystoreConfig.getPassword().toCharArray()
        );

        return factory.getKeyPair(
                keystoreConfig.getAlias(),
                keystoreConfig.getKeyPassword().toCharArray()
        );
    }

}
