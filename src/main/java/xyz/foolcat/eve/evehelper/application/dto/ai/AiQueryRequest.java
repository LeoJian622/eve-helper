package xyz.foolcat.eve.evehelper.application.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI查询请求DTO
 */
@Data
@Schema(description = "AI查询请求")
public class AiQueryRequest {

    @NotBlank(message = "查询问题不能为空")
    @Schema(description = "用户自然语言问题", example = "查询所有价格超过100万ISK的市场订单")
    private String question;

    @Schema(description = "会话ID，用于上下文连续查询（可选）", example = "abc123xyz")
    private String sessionId;

    @Schema(description = "是否包含Schema说明", example = "false")
    private boolean includeSchema = false;
}
