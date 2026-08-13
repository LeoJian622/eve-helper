package xyz.foolcat.eve.evehelper.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Refresh Token请求DTO
 *
 * @author Leojan
 * date 2026-01-30
 */
@Data
@Schema(description = "刷新Token请求")
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken不能为空")
    // 008 R4:@Size 拒绝超长载荷(防 DoS/CRLF 污染);@Pattern 与 AuthApplicationService.UUID_PATTERN 同源
    @Size(max = 64, message = "refreshToken长度不能超过64")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "refreshToken必须为UUID格式")
    @Schema(description = "刷新令牌", required = true)
    private String refreshToken;
}
