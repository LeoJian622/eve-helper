package xyz.foolcat.eve.evehelper.application.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RefreshTokenRequest 边界校验测试(008 T012 / research R4)。
 *
 * <p>验证 {@code @Size(max=64)} 拒绝超长载荷(防 DoS/CRLF 污染),{@code @Pattern} 拒绝非 UUID
 * (与 {@code AuthApplicationService.UUID_PATTERN} 同源),合法 36 字符 UUID 不误伤。
 * 校验在 DTO 边界发生,垃圾载荷不触达 {@code AuthApplicationService}(MEDIUM-3)。
 */
class RefreshTokenRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<ConstraintViolation<RefreshTokenRequest>> validate(String token) {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(token);
        return validator.validate(req);
    }

    @Test
    void oversizedTokenRejectedBySize() {
        // 65 字符(超 max=64)应被 @Size 拒绝
        String oversized = "a".repeat(65);
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validate(oversized);

        assertFalse(violations.isEmpty(), "超长 refreshToken 必须在 DTO 边界被拒");
    }

    @Test
    void nonUuidRejectedByPattern() {
        // 非 UUID 形状应被 @Pattern 拒绝
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validate("not-a-uuid-token-at-all");

        assertFalse(violations.isEmpty(), "非 UUID refreshToken 必须在 DTO 边界被拒");
    }

    @Test
    void validUuidAccepted() {
        // 合法 36 字符 UUID 不误伤
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validate("550e8400-e29b-41d4-a716-446655440000");

        assertTrue(violations.isEmpty(), "合法 UUID 不应被拒绝: " + violations);
    }
}