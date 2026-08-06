package xyz.foolcat.eve.evehelper.domain.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Token 对领域读模型。
 *
 * <p>领域服务生成此读模型承载 Access/Refresh Token 数据,
 * 由 application/interfaces 层直接作为响应载荷返回。</p>
 *
 * @param accessToken  访问令牌(带 Bearer 前缀)
 * @param refreshToken 刷新令牌
 * @param expiresIn    访问令牌过期时间(秒)
 * @param tokenType    令牌类型
 * @author Leojan
 */
@Schema(description = "Token对")
public record TokenResult(

        @Schema(description = "访问令牌(带Bearer前缀)", example = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "刷新令牌", example = "550e8400-e29b-41d4-a716-446655440000")
        String refreshToken,

        @Schema(description = "访问令牌过期时间(秒)", example = "900")
        Long expiresIn,

        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType) {
}
