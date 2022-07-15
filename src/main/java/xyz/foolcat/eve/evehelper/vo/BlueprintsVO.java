package xyz.foolcat.eve.evehelper.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
    * 蓝图属性
    */
@ApiModel(value="蓝图属性")
@Data
public class BlueprintsVO implements Serializable {
    /**
     * 唯一item id
     */
    @ApiModelProperty(value="唯一item id")
    private Long itemId;

    /**
     * invType id
     */
    @ApiModelProperty(value="invType id")
    private Integer typeId;

    /**
     * TypeName
     */
    @ApiModelProperty(value = "TypeName")
    private String typeName;

    /**
     * 材料效率
     */
    @ApiModelProperty(value="材料效率")
    private Integer materialEfficiency;

    /**
     * 时间效率
     */
    @ApiModelProperty(value="时间效率")
    private Integer timeEfficiency;

    /**
     * 流程数
     */
    @ApiModelProperty(value="流程数")
    private Integer runs;

    /**
     * 蓝图拷贝
     */
    @ApiModelProperty(value = "蓝图拷贝")
    private Boolean isBlueprintCopy;

    /**
     * 数量
     */
    @ApiModelProperty(value="数量")
    private Long quantity;

    /**
     * 建筑ID
     */
    @ApiModelProperty(value="建筑ID")
    private Long locationId;

    /**
     * 建筑ID
     */
    @ApiModelProperty(value = "建筑名")
    private Long name;

    /**
     * 位置
     */
    @ApiModelProperty(value="位置")
    private String locationFlag;

    /**
     * 所有者ID
     */
    @ApiModelProperty(value="所有者ID")
    private Long ownerId;

    /**
     * 所有者
     */
    @ApiModelProperty(value = "所有者")
    private String owner;

}