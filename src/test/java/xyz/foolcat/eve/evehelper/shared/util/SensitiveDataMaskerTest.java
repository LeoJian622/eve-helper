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

    // ---- 控制字符免疫(R5 / FR-008 / LOW-2s 的 masker 部分)----

    @Test
    void testMaskToken_stripsControlCharacters() {
        // 控制字符(NUL)落在 prefix 可见区(日志注入面);剔除后仅剩可见掩码
        // 输入为 a + NUL + bcdefgh,长度9:prefix="a"+NUL+"bc" -> 剔后"abc", suffix="efgh"
        String masked = SensitiveDataMasker.maskToken("a\u0000bcdefgh");

        assertNoControlCharacters(masked);
        assertEquals("abc****efgh", masked);
    }

    @Test
    void testMaskToken_nullByteAndCrlfOnly() {
        // 纯控制字符输入(CR LF NUL ESC,长度<8 走短分支)不应产生可伪造日志行的输出
        String masked = SensitiveDataMasker.maskToken("\r\n\u0000\u001B");

        assertNoControlCharacters(masked);
    }

    @Test
    void testMaskUsername_stripsControlCharacters() {
        // 控制字符(NUL)落在 prefix 可见区(maskUsername prefix=前2)
        // 输入为 a + NUL + minx:prefix="a"+NUL -> 剔后"a", suffix="x"
        String masked = SensitiveDataMasker.maskUsername("a\u0000minx");

        assertNoControlCharacters(masked);
        assertEquals("a****x", masked);
    }

    @Test
    void testMaskEmail_stripsControlCharactersInDomain() {
        // CRLF 注入点在域名段(日志注入面)
        String masked = SensitiveDataMasker.maskEmail("u@example\r\n.com");

        assertNoControlCharacters(masked);
        assertEquals("u***@example.com", masked);
    }

    @Test
    void testValidInputOutputUnchanged() {
        // 合法输入(无控制字符)掩码规则不变
        assertEquals("550e****0000", SensitiveDataMasker.maskToken("550e8400-e29b-41d4-a716-446655440000"));
        assertEquals("ad****3", SensitiveDataMasker.maskUsername("admin123"));
        assertEquals("u***@example.com", SensitiveDataMasker.maskEmail("user@example.com"));
    }

    private static void assertNoControlCharacters(String value) {
        assertTrue(value.chars().noneMatch(Character::isISOControl),
                "输出不得包含任何控制字符: " + value);
    }
}