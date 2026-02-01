package xyz.foolcat.eve.evehelper.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感数据脱敏工具类测试
 *
 * @author Leojan
 * date 2026-02-01
 */
class SensitiveDataMaskerTest {

    @Test
    void testMaskToken_normalToken() {
        // 测试正常的UUID格式token
        String token = "550e8400-e29b-41d4-a716-446655440000";
        String masked = SensitiveDataMasker.maskToken(token);

        assertEquals("550e****0000", masked);
        assertTrue(masked.contains("****"));
        assertEquals(12, masked.length());
    }

    @Test
    void testMaskToken_longToken() {
        // 测试长JWT token
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature";
        String masked = SensitiveDataMasker.maskToken(token);

        assertEquals("eyJh****ture", masked);
        assertTrue(masked.startsWith("eyJh"));
        assertTrue(masked.endsWith("ture"));
    }

    @Test
    void testMaskToken_shortToken() {
        // 测试短token(少于8个字符)
        String token = "abc123";
        String masked = SensitiveDataMasker.maskToken(token);

        assertEquals("***", masked);
    }

    @Test
    void testMaskToken_nullToken() {
        // 测试null
        String masked = SensitiveDataMasker.maskToken(null);

        assertEquals("***", masked);
    }

    @Test
    void testMaskToken_emptyToken() {
        // 测试空字符串
        String masked = SensitiveDataMasker.maskToken("");

        assertEquals("***", masked);
    }

    @Test
    void testMaskUsername_normalUsername() {
        // 测试正常用户名
        String username = "admin123";
        String masked = SensitiveDataMasker.maskUsername(username);

        assertEquals("ad****3", masked);
        assertTrue(masked.startsWith("ad"));
        assertTrue(masked.endsWith("3"));
    }

    @Test
    void testMaskUsername_shortUsername() {
        // 测试短用户名(少于3个字符)
        String username = "ab";
        String masked = SensitiveDataMasker.maskUsername(username);

        assertEquals("***", masked);
    }

    @Test
    void testMaskUsername_nullUsername() {
        // 测试null
        String masked = SensitiveDataMasker.maskUsername(null);

        assertEquals("***", masked);
    }

    @Test
    void testMaskEmail_normalEmail() {
        // 测试正常邮箱
        String email = "user@example.com";
        String masked = SensitiveDataMasker.maskEmail(email);

        assertEquals("u***@example.com", masked);
        assertTrue(masked.contains("@example.com"));
    }

    @Test
    void testMaskEmail_longEmail() {
        // 测试长邮箱
        String email = "verylongemail@example.com";
        String masked = SensitiveDataMasker.maskEmail(email);

        assertEquals("v***@example.com", masked);
    }

    @Test
    void testMaskEmail_invalidEmail() {
        // 测试无效邮箱(没有@符号)
        String email = "notanemail";
        String masked = SensitiveDataMasker.maskEmail(email);

        assertEquals("***", masked);
    }

    @Test
    void testMaskEmail_nullEmail() {
        // 测试null
        String masked = SensitiveDataMasker.maskEmail(null);

        assertEquals("***", masked);
    }
}
