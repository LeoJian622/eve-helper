package xyz.foolcat.eve.evehelper.domain.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
    * 蓝图属性
    */
@Schema(description="蓝图属性")
@Data
public class BlueprintsVO implements Serializable {
    /**
     * 唯一item id
     */
    @Schema(description="唯一item id")
    private Long itemId;

    /**
     * invType id
     */
    @Schema(description="invType id")
    private Integer typeId;

    /**
     * Typedescription
     */
    @Schema(name="TypeName")
    private String typeName;

    /**
     * 材料效率
     */
    @Schema(description="材料效率")
    private Integer materialEfficiency;

    /**
     * 时间效率
     */
    @Schema(description="时间效率")
    private Integer timeEfficiency;

    /**
     * 流程数
     */
    @Schema(description="流程数")
    private Integer runs;

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
     * 位置
     */
    @Schema(description="位置")
    private String locationFlag;

    /**
     * 所有者ID
     */
    @Schema(description="所有者ID")
    private Long ownerId;

    /**
     * 所有者
     */
    @Schema(description="所有者")
    private String owner;

}