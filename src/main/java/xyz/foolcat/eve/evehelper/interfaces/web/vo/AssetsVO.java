package xyz.foolcat.eve.evehelper.interfaces.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema(title="资产视图")
@Data
public class AssetsVO implements Serializable {
    /**
     * 物品ID
     */
    @Schema(name="物品ID")
    private Long itemId;

    /**
     * invTypeID
     */
    @Schema(name="invTypeID")
    private Integer typeId;

    /**
     * TypeName
     */
    @Schema(name="TypeName")
    private String typeName;

    /**
     * 建筑ID
     */
    @Schema(name="建筑ID")
    private Long locationId;

    /**
     * 建筑ID
     */
    @Schema(name="建筑名")
    private Long name;

    /**
     * 类型station, solar_system, item, other
     */
    @Schema(name="类型station, solar_system, item, other")
    private String locationType;

    @Schema(name="")
    private String locationFlag;

    @Schema(name="")
    private Boolean isSingleton;

    /**
     * 蓝图拷贝
     */
    @Schema(name="蓝图拷贝")
    private Boolean isBlueprintCopy;

    /**
     * 数量
     */
    @Schema(name="数量")
    private Long quantity;

    /**
     * 所有者
     */
    @Schema(name="所有者")
    private String owner;

    /**
     * 所有者ID
     */
    @Schema(name="所有者ID")
    private String ownerId;

}