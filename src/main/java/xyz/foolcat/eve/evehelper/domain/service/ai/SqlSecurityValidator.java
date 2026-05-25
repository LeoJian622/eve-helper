package xyz.foolcat.eve.evehelper.domain.service.ai;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * SQL安全校验器
 * 确保AI生成的SQL只包含只读操作，不包含危险操作
 */
@Slf4j
@Component
public class SqlSecurityValidator {

    /**
     * 允许的关键字（只读操作）
     */
    private static final List<String> ALLOWED_KEYWORDS = Arrays.asList(
            "SELECT", "FROM", "WHERE", "JOIN", "LEFT JOIN", "INNER JOIN",
            "ON", "AND", "OR", "ORDER BY", "GROUP BY", "HAVING", "LIMIT",
            "AS", "DISTINCT", "COUNT", "SUM", "AVG", "MAX", "MIN"
    );

    /**
     * 禁止的关键字（危险操作）
     */
    private static final List<String> FORBIDDEN_KEYWORDS = Arrays.asList(
            "DELETE", "DROP", "ALTER", "TRUNCATE", "INSERT", "UPDATE",
            "CREATE", "REPLACE", "GRANT", "REVOKE", "EXEC", "EXECUTE", "CALL"
    );

    /**
     * 校验SQL语句
     *
     * @param sql SQL语句
     * @return 校验结果
     */
    public ValidationResult validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return ValidationResult.fail("SQL语句为空");
        }

        String upperSql = sql.toUpperCase().trim();

        // 检查禁止的关键字
        for (String forbidden : FORBIDDEN_KEYWORDS) {
            if (upperSql.contains(forbidden)) {
                String errorMsg = String.format("SQL包含禁止的关键字: %s", forbidden);
                log.warn("SQL validation failed: {}", errorMsg);
                return ValidationResult.fail(errorMsg);
            }
        }

        // 验证语法正确性
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);

            // 确保是SELECT语句
            if (!(statement instanceof Select)) {
                return ValidationResult.fail("只允许SELECT查询语句");
            }

        } catch (JSQLParserException e) {
            log.warn("SQL语法解析失败: {}", e.getMessage());
            return ValidationResult.fail("SQL语法错误: " + e.getMessage());
        }

        return ValidationResult.success();
    }

    /**
     * 确保SQL语句包含LIMIT限制
     *
     * @param sql       原始SQL
     * @param maxResult 最大返回条数
     * @return 包含LIMIT的SQL
     */
    public String ensureLimit(String sql, int maxResult) {
        String upperSql = sql.toUpperCase().trim();

        // 检查是否已有LIMIT
        if (upperSql.contains("LIMIT")) {
            // 提取当前LIMIT值
            try {
                int limitIndex = upperSql.lastIndexOf("LIMIT");
                String limitPart = upperSql.substring(limitIndex + 5).trim();
                int currentLimit = Integer.parseInt(limitPart.split("\\s+|;")[0].trim());
                if (currentLimit > maxResult) {
                    // 替换LIMIT为maxResult
                    return sql.substring(0, sql.toUpperCase().lastIndexOf("LIMIT"))
                            + " LIMIT " + maxResult;
                }
                return sql;
            } catch (Exception e) {
                log.warn("解析LIMIT失败，重新添加: {}", e.getMessage());
            }
        }

        // 移除末尾的分号（如果有）
        String cleanSql = sql.trim();
        if (cleanSql.endsWith(";")) {
            cleanSql = cleanSql.substring(0, cleanSql.length() - 1);
        }

        return cleanSql + " LIMIT " + maxResult;
    }

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
