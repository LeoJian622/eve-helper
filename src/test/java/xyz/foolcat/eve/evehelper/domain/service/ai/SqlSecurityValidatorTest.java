package xyz.foolcat.eve.evehelper.domain.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL安全校验器单元测试
 */
class SqlSecurityValidatorTest {

    private SqlSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SqlSecurityValidator();
    }

    @Test
    @DisplayName("合法SELECT语句应该通过校验")
    void shouldPassValidSelectStatement() {
        String sql = "SELECT * FROM market_order WHERE price > 1000000 LIMIT 50";
        SqlSecurityValidator.ValidationResult result = validator.validate(sql);
        assertTrue(result.isValid(), result.getErrorMessage());
    }

    @Test
    @DisplayName("包含DELETE的语句应该被拒绝")
    void shouldRejectDeleteStatement() {
        String sql = "DELETE FROM market_order WHERE id = 1";
        SqlSecurityValidator.ValidationResult result = validator.validate(sql);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("DELETE"));
    }

    @Test
    @DisplayName("包含DROP的语句应该被拒绝")
    void shouldRejectDropStatement() {
        String sql = "DROP TABLE market_order";
        SqlSecurityValidator.ValidationResult result = validator.validate(sql);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("DROP"));
    }

    @Test
    @DisplayName("包含UPDATE的语句应该被拒绝")
    void shouldRejectUpdateStatement() {
        String sql = "UPDATE market_order SET price = 1000000 WHERE id = 1";
        SqlSecurityValidator.ValidationResult result = validator.validate(sql);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("UPDATE"));
    }

    @Test
    @DisplayName("包含INSERT的语句应该被拒绝")
    void shouldRejectInsertStatement() {
        String sql = "INSERT INTO market_order (id, price) VALUES (1, 1000000)";
        SqlSecurityValidator.ValidationResult result = validator.validate(sql);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("INSERT"));
    }

    @Test
    @DisplayName("无效SQL语法应该被拒绝")
    void shouldRejectInvalidSqlSyntax() {
        String sql = "SELEKT * FROM market_order";
        SqlSecurityValidator.ValidationResult result = validator.validate(sql);
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("应该自动添加LIMIT 50到没有LIMIT的语句")
    void shouldAddLimitToStatement() {
        String sql = "SELECT * FROM market_order WHERE price > 1000000";
        String result = validator.ensureLimit(sql, 50);
        assertTrue(result.toUpperCase().contains("LIMIT"));
        assertTrue(result.endsWith("50"));
    }

    @Test
    @DisplayName("已有LIMIT的语句应该被截断为50")
    void shouldTruncateExistingLimitTo50() {
        String sql = "SELECT * FROM market_order LIMIT 100";
        String result = validator.ensureLimit(sql, 50);
        assertTrue(result.contains("LIMIT 50"));
        assertFalse(result.contains("LIMIT 100"));
    }

    @Test
    @DisplayName("子查询应该被正确识别限制")
    void shouldHandleSubQueries() {
        String sql = "SELECT * FROM (SELECT * FROM market_order WHERE price > 1000000) t";
        String result = validator.ensureLimit(sql, 50);
        assertTrue(result.contains("LIMIT 50"));
    }
}
