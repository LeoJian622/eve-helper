package xyz.foolcat.eve.evehelper.infrastructure.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI查询配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-query")
public class AiQueryProperties {

    /**
     * 最大返回结果数
     */
    private int maxResultLimit = 50;

    /**
     * 查询超时时间（毫秒）
     */
    private long queryTimeoutMs = 10000;

    /**
     * 会话缓存过期时间（分钟）
     */
    private int sessionTtlMinutes = 30;

    /**
     * 允许查询的表名称白名单
     */
    private List<String> allowedTables;

    /**
     * 允许的SQL关键字
     */
    private List<String> allowedKeywords = List.of(
            "SELECT", "FROM", "WHERE", "JOIN", "LEFT JOIN", "INNER JOIN",
            "ON", "AND", "OR", "ORDER BY", "GROUP BY", "HAVING", "LIMIT",
            "AS", "DISTINCT", "COUNT", "SUM", "AVG", "MAX", "MIN"
    );

    /**
     * 禁止的SQL关键字
     */
    private List<String> forbiddenKeywords = List.of(
            "DELETE", "DROP", "ALTER", "TRUNCATE", "INSERT", "UPDATE",
            "CREATE", "REPLACE", "GRANT", "REVOKE", "EXEC", "EXECUTE", "CALL"
    );
}
