package xyz.foolcat.eve.evehelper.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

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
    @Schema(description = "刷新令牌", required = true)
    private String refreshToken;
}
