package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * 蓝图属性
    */
@ApiModel(value="蓝图属性")
@Data
@TableName(value = "blueprints")
public class Blueprints  implements Serializable {
    /**
     * 唯一item id
     */
    @TableId(value = "item_id")
    @ApiModelProperty(value="唯一item id")
    private Long itemId;

    /**
     * invType id
     */
    @TableField(value = "type_id")
    @ApiModelProperty(value="invType id")
    private Integer typeId;

    /**
     * 材料效率
     */
    @TableField(value = "material_efficiency")
    @ApiModelProperty(value="材料效率")
    private Integer materialEfficiency;

    /**
     * 时间效率
     */
    @TableField(value = "time_efficiency")
    @ApiModelProperty(value="时间效率")
    private Integer timeEfficiency;

    /**
     * 流程数
     */
    @TableField(value = "runs")
    @ApiModelProperty(value="流程数")
    private Integer runs;

    /**
     * 数量
     */
    @TableField(value = "quantity")
    @ApiModelProperty(value="数量")
    private Long quantity;

    /**
     * 建筑ID
     */
    @TableField(value = "location_id")
    @ApiModelProperty(value="建筑ID")
    private Long locationId;

    /**
     * 位置
     */
    @TableField(value = "location_flag")
    @ApiModelProperty(value="位置")
    private String locationFlag;

    /**
     * 所有者ID
     */
    @TableField(value = "owner_id")
    @ApiModelProperty(value="所有者ID")
    private Long ownerId;

    public static final String COL_ITEM_ID = "item_id";

    public static final String COL_TYPE_ID = "type_id";

    public static final String COL_MATERIAL_EFFICIENCY = "material_efficiency";

    public static final String COL_TIME_EFFICIENCY = "time_efficiency";

    public static final String COL_RUNS = "runs";

    public static final String COL_QUANTITY = "quantity";

    public static final String COL_LOCATION_ID = "location_id";

    public static final String COL_LOCATION_FLAG = "location_flag";

    public static final String COL_OWNER_ID = "owner_id";
}