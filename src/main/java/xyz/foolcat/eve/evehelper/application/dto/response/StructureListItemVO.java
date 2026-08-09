package xyz.foolcat.eve.evehelper.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑列表项响应 VO
 *
 * @author Leojan
 */
@Data
@Schema(description = "建筑列表项")
public class StructureListItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "建筑ID")
    private Long structureId;

    @Schema(description = "建筑名称")
    private String name;

    @Schema(description = "建筑类型ID")
    private Long typeId;

    @Schema(description = "建筑类型名")
    private String typeName;

    @Schema(description = "星系ID")
    private Long systemId;

    @Schema(description = "星系名")
    private String systemName;

    @Schema(description = "状态")
    private String state;

    @Schema(description = "燃料到期时间")
    private OffsetDateTime fuelExpires;

    @Schema(description = "军团ID")
    private Long corporationId;
}
