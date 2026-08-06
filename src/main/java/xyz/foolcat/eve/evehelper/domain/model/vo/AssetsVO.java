package xyz.foolcat.eve.evehelper.domain.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema(description="资产视图")
@Data
public class AssetsVO implements Serializable {
    /**
     * 物品ID
     */
    @Schema(description="物品ID")
    private Long itemId;

    /**
     * invTypeID
     */
    @Schema(description="invTypeID")
    private Integer typeId;

    /**
     * TypeName
     */
    @Schema(description="TypeName")
    private String typeName;

    /**
     * 建筑ID
     */
    @Schema(description="建筑ID")
    private Long locationId;

    /**
     * 建筑ID
     */
    @Schema(description="建筑名")
    private Long name;

    /**
     * 类型station, solar_system, item, other
     */
    @Schema(description="类型station, solar_system, item, other")
    private String locationType;

    @Schema(description="")
    private String locationFlag;

    @Schema(description="")
    private Boolean isSingleton;

    /**
     * 蓝图拷贝
     */
    @Schema(description="蓝图拷贝")
    private Boolean isBlueprintCopy;

    /**
     * 数量
     */
    @Schema(description="数量")
    private Long quantity;

    /**
     * 所有者
     */
    @Schema(description="所有者")
    private String owner;

    /**
     * 所有者ID
     */
    @Schema(description="所有者ID")
    private String ownerId;

}