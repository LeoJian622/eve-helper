package xyz.foolcat.eve.evehelper.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
    * 蓝图属性
    */
@Schema(title="蓝图属性")
@Data
public class BlueprintsDTO implements Serializable {
    /**
     * 唯一item id
     */
    @Schema(name="唯一item id")
    private Long itemId;

    /**
     * 物品类型id
     */
    @Schema(name="物品类型id")
    private Integer typeId;

    /**
     * 物品名称
     */
    @Schema(name="物品名称")
    private String typeName;

    /**
     * 材料效率
     */
    @Schema(name="材料效率")
    private Integer materialEfficiency;

    /**
     * 时间效率
     */
    @Schema(name="时间效率")
    private Integer timeEfficiency;

    /**
     * 流程数
     */
    @Schema(name="流程数")
    private Integer runs;

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
     * 位置
     */
    @Schema(name="位置")
    private String locationFlag;

    /**
     * 所有者ID
     */
    @Schema(name="所有者ID")
    private Long ownerId;

    /**
     * 所有者
     */
    @Schema(name="所有者")
    private String owner;

}