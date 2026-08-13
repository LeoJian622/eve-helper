package xyz.foolcat.eve.evehelper.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CharacterAccessTokenResult} 脱敏契约测试。
 *
 * <p>Java record 自动生成的 toString() 会输出全部组件,包含 accessToken 明文。
 * 一旦有人写下 log.debug("result={}", result),凭证即随日志泄露。
 * 本测试把脱敏固化在类型上,而非依赖每个调用方自律(006 M-NEW-1)。</p>
 *
 * @author Leojan
 * date 2026-08-11
 */
@DisplayName("CharacterAccessTokenResult 脱敏契约")
class CharacterAccessTokenResultTest {

    /**
     * 形如真实 ESI accessToken 的取样:带 Bearer 前缀的 JWT。
     * 签名段为字面量,非真实签名。
     */
    private static final String SAMPLE_TOKEN =
            "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJDSEFSQUNURVI6RVZFOjk1NDY1NDk5In0.sig";

    @Test
    @DisplayName("toString 不得包含 accessToken 任何片段")
    void toString_neverExposesAccessToken() {
        // Arrange
        CharacterAccessTokenResult result =
                new CharacterAccessTokenResult(SAMPLE_TOKEN, 95465499, 1140L);

        // Act
        String rendered = result.toString();

        // Assert:整串、JWT 载荷段、签名段均不得出现
        assertFalse(rendered.contains(SAMPLE_TOKEN),
                "toString 泄露了完整 accessToken: " + rendered);
        assertFalse(rendered.contains("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"),
                "toString 泄露了 JWT header 段: " + rendered);
        assertFalse(rendered.contains("eyJzdWIiOiJDSEFSQUNURVI6RVZFOjk1NDY1NDk5In0"),
                "toString 泄露了 JWT payload 段: " + rendered);
        assertFalse(rendered.contains(".sig"),
                "toString 泄露了 JWT 签名段: " + rendered);
    }

    @Test
    @DisplayName("toString 保留 characterId 与 expiresIn 以便排查")
    void toString_keepsNonSensitiveFields() {
        // Arrange
        CharacterAccessTokenResult result =
                new CharacterAccessTokenResult(SAMPLE_TOKEN, 95465499, 1140L);

        // Act
        String rendered = result.toString();

        // Assert:脱敏不等于无用,非敏感字段须可见
        assertTrue(rendered.contains("95465499"),
                "toString 应保留 characterId 便于排查: " + rendered);
        assertTrue(rendered.contains("1140"),
                "toString 应保留 expiresIn 便于排查: " + rendered);
    }

    @Test
    @DisplayName("accessToken 为 null 时 toString 不抛异常")
    void toString_handlesNullTokenGracefully() {
        // Arrange
        CharacterAccessTokenResult result =
                new CharacterAccessTokenResult(null, 95465499, 0L);

        // Act
        String rendered = result.toString();

        // Assert
        assertTrue(rendered.contains("95465499"), "null token 时仍应渲染 characterId: " + rendered);
    }

    @Test
    @DisplayName("accessToken 访问器仍返回原值,脱敏只作用于 toString")
    void accessor_stillReturnsRawToken() {
        // Arrange
        CharacterAccessTokenResult result =
                new CharacterAccessTokenResult(SAMPLE_TOKEN, 95465499, 1140L);

        // Act & Assert:脱敏不得破坏接口契约——响应体依赖该访问器返回真实令牌
        assertEquals(SAMPLE_TOKEN, result.accessToken(),
                "脱敏 toString 不应影响 accessToken() 返回值");
    }
}
