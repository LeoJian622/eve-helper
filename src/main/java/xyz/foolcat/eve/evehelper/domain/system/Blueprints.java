package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 蓝图属性
 */
@Schema(description = "蓝图属性")
@Data
@TableName(value = "blueprints")
public class Blueprints implements Serializable {
    /**
     * 唯一item id
     */
    @TableId(value = "item_id")
    @Schema(description = "唯一item id")
    private Long itemId;

    /**
     * invType id
     */
    @TableField(value = "type_id")
    @Schema(description = "invType id")
    private Integer typeId;

    /**
     * 材料效率
     */
    @TableField(value = "material_efficiency")
    @Schema(description = "材料效率")
    private Integer materialEfficiency;

    /**
     * 时间效率
     */
    @TableField(value = "time_efficiency")
    @Schema(description = "时间效率")
    private Integer timeEfficiency;

    /**
     * 流程数
     */
    @TableField(value = "runs")
    @Schema(description = "流程数")
    private Integer runs;

    /**
     * 数量
     */
    @TableField(value = "quantity")
    @Schema(description = "数量")
    private Long quantity;

    /**
     * 建筑ID
     */
    @TableField(value = "location_id")
    @Schema(description = "建筑ID")
    private Long locationId;

    /**
     * 位置
     */
    @TableField(value = "location_flag")
    @Schema(description = "位置")
    private String locationFlag;

    /**
     * 所有者ID
     */
    @TableField(value = "owner_id")
    @Schema(description = "所有者ID")
    private Long ownerId;

    private static final long serialVersionUID = 1L;

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