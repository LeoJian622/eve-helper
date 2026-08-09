package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑燃料预警响应 VO
 *
 * @author Leojan
 */
@Data
@Schema(description = "建筑燃料预警")
public class StructureFuelVO implements Serializable {

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

    @Schema(description = "燃料到期时间")
    private OffsetDateTime fuelExpires;

    @Schema(description = "剩余时长(小时)")
    private Long remainingHours;
}
