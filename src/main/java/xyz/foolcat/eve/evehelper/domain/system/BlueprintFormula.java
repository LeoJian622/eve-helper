package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
    * 蓝图材料配方基础数据
    */
@ApiModel(value="蓝图材料配方基础数据")
@Data
@TableName(value = "blueprint_formula")
public class BlueprintFormula implements Serializable {
    /**
     * 蓝图ID
     */
    @TableField(value = "blueprint_id")
    @ApiModelProperty(value="蓝图ID")
    private Integer blueprintId;

    /**
     * 原材料typeid
     */
    @TableField(value = "material_type_id")
    @ApiModelProperty(value="原材料typeid")
    private Integer materialTypeId;

    /**
     * 名称
     */
    @TableField(value = "type_name")
    @ApiModelProperty(value="名称")
    private String typeName;

    /**
     * 活动类型
     */
    @TableField(value = "activity_id")
    @ApiModelProperty(value="活动类型")
    private Byte activityId;

    /**
     * 需求数量
     */
    @TableField(value = "quantity")
    @ApiModelProperty(value="需求数量")
    private Integer quantity;

    private static final long serialVersionUID = 1L;

    public static final String COL_BLUEPRINT_ID = "blueprint_id";

    public static final String COL_MATERIAL_TYPE_ID = "material_type_id";

    public static final String COL_TYPE_NAME = "type_name";

    public static final String COL_ACTIVITY_ID = "activity_id";

    public static final String COL_QUANTITY = "quantity";
}