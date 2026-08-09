package xyz.foolcat.eve.evehelper.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建筑燃料预警查询条件
 *
 * @author Leojan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureFuelQuery {

    /**
     * 军团ID
     */
    @NotBlank(message = "军团ID不能为空")
    private String corporationId;

    /**
     * 预警时长(小时),筛选该时长内燃料耗尽的建筑
     */
    @Min(value = 1, message = "预警时长不能小于1小时")
    @Max(value = 720, message = "预警时长不能超过720小时")
    @Builder.Default
    private Integer hours = 72;
}
