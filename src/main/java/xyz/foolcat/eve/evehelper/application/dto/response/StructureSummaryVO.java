package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 建筑统计概览响应 VO(应用层汇总)
 *
 * @author Leojan
 */
@Data
@Schema(description = "建筑统计概览")
public class StructureSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "建筑总数")
    private Long total;

    @Schema(description = "已缺油建筑数(fuel_expires 为空)")
    private Long fuelExpiredCount;

    @Schema(description = "即将缺油建筑数(72 小时内)")
    private Long lowFuelCount;

    @Schema(description = "各状态计数")
    private Map<String, Long> stateCounts;
}
