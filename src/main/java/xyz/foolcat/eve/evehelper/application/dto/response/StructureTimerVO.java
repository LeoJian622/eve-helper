package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑 增强/解锚时间提醒响应 VO
 *
 * @author Leojan
 */
@Data
@Schema(description = "建筑增强/解锚时间提醒")
public class StructureTimerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "建筑ID")
    private Long structureId;

    @Schema(description = "建筑名称")
    private String name;

    @Schema(description = "建筑类型名")
    private String typeName;

    @Schema(description = "星系名")
    private String systemName;

    @Schema(description = "状态")
    private String state;

    @Schema(description = "增强小时")
    private Integer reinforceHour;

    @Schema(description = "下次增强小时")
    private Integer nextReinforceHour;

    @Schema(description = "下次增强时间")
    private OffsetDateTime nextReinforceApply;

    @Schema(description = "状态开始时间")
    private OffsetDateTime stateTimerStart;

    @Schema(description = "状态结束时间")
    private OffsetDateTime stateTimerEnd;

    @Schema(description = "解锚时间")
    private OffsetDateTime unanchorsAt;
}
