package xyz.foolcat.eve.evehelper.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 多数据源占位符回归测试。
 * <p>
 * 历史缺陷:application.yml 中 eve 数据源 username 误用 ${DB_SYSTEM_USERNAME},
 * 导致 dev/pro 环境下 eve 库使用 system 库凭据连接(仅当两库用户不同时暴露;
 * test 环境因 application-test.yml 硬编码同一用户而未暴露)。此测试防止该笔误回归。
 */
class DataSourceConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    void eveDatasourceUsesDedicatedEveCredentials() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertNotNull(in, "application.yml 必须存在于 classpath");

            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
            Map<String, Object> druid = (Map<String, Object>) datasource.get("druid");

            Map<String, Object> system = (Map<String, Object>) druid.get("system");
            Map<String, Object> eve = (Map<String, Object>) druid.get("eve");

            assertEquals("${DB_SYSTEM_USERNAME}", system.get("username"),
                    "system 数据源应使用 DB_SYSTEM_USERNAME");
            assertEquals("${DB_SYSTEM_PASSWORD}", system.get("password"),
                    "system 数据源应使用 DB_SYSTEM_PASSWORD");

            assertEquals("${DB_EVE_USERNAME}", eve.get("username"),
                    "eve 数据源应使用 DB_EVE_USERNAME,而非 DB_SYSTEM_USERNAME");
            assertEquals("${DB_EVE_PASSWORD}", eve.get("password"),
                    "eve 数据源应使用 DB_EVE_PASSWORD");
        }
    }
}
