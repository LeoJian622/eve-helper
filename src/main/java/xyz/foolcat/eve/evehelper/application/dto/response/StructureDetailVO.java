package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.StructureService;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 建筑详情响应 VO
 *
 * @author Leojan
 */
@Data
@Schema(description = "建筑详情")
public class StructureDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "建筑ID")
    private Long structureId;

    @Schema(description = "军团ID")
    private Long corporationId;

    @Schema(description = "燃料到期时间")
    private OffsetDateTime fuelExpires;

    @Schema(description = "建筑名称")
    private String name;

    @Schema(description = "下次增强时间")
    private OffsetDateTime nextReinforceApply;

    @Schema(description = "下次增强小时")
    private Integer nextReinforceHour;

    @Schema(description = "方案ID")
    private Long profileId;

    @Schema(description = "增强小时")
    private Integer reinforceHour;

    @Schema(description = "状态")
    private String state;

    @Schema(description = "状态结束时间")
    private OffsetDateTime stateTimerEnd;

    @Schema(description = "状态开始时间")
    private OffsetDateTime stateTimerStart;

    @Schema(description = "星系ID")
    private Long systemId;

    @Schema(description = "物品类型ID")
    private Long typeId;

    @Schema(description = "解锚时间")
    private OffsetDateTime unanchorsAt;

    @Schema(description = "建筑类型名")
    private String typeName;

    @Schema(description = "星系名")
    private String systemName;

    @Schema(description = "建筑服务列表")
    private List<StructureService> services;
}
