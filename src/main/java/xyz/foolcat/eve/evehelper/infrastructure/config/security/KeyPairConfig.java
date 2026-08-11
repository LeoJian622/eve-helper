package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
        // 007 T023/T024:location 默认值已移除,缺失必须 fail-fast 拒启
        if (!StringUtils.hasText(keystoreConfig.getLocation())) {
            throw new IllegalStateException("密钥库位置未配置,请设置环境变量 KEYSTORE_LOCATION");
        }

        Resource resource = resolveResource(keystoreConfig.getLocation());
        // 007 T023:日志只打文件名不打完整路径(LOW-5,防生产密钥路径泄露)
        log.info("正在从密钥库加载密钥对: {}", resource.getFilename());

        KeyStoreKeyFactory factory = new KeyStoreKeyFactory(
                resource,
                keystoreConfig.getPassword().toCharArray()
        );

        return factory.getKeyPair(
                keystoreConfig.getAlias(),
                keystoreConfig.getKeyPassword().toCharArray()
        );
    }

    /**
     * 007 T023(FR-005~007):加载路径分派 ——
     * {@code classpath:} 前缀 → ClassPathResource(仅 test profile 合法,
     * SecurityBaselineValidator 对其余 profile fail-closed 拒启);
     * 其余一律按文件系统路径处理(生产密钥必须外置,绝不随 jar 分发)。
     */
    private static Resource resolveResource(String location) {
        if (location.startsWith("classpath:")) {
            return new ClassPathResource(location.substring("classpath:".length()));
        }
        return new FileSystemResource(location);
    }

}
