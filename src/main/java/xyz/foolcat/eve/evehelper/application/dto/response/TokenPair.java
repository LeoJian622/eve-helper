package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token对响应DTO
 * 包含access token和refresh token
 *
 * @author Leojan
 * date 2026-01-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token对")
public class TokenPair {

    @Schema(description = "访问令牌(带Bearer前缀)", example = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;

    @Schema(description = "访问令牌过期时间(秒)", example = "900")
    private Long expiresIn;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";
}
