package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 安全配置属性
 * 用于从环境变量加载敏感配置信息
 *
 * @author Leojan
 * date 2026-01-30
 */
@Configuration
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {

    private Keystore keystore = new Keystore();

    @Data
    public static class Keystore {
        /**
         * 密钥库文件位置
         */
        private String location = "eve-jwt.jks";

        /**
         * 密钥库密码(从环境变量加载)
         */
        private String password;

        /**
         * 密钥别名
         */
        private String alias = "eve-jwt";

        /**
         * 密钥密码(从环境变量加载)
         */
        private String keyPassword;
    }
}
