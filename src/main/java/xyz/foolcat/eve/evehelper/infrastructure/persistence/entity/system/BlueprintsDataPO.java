package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 蓝图生产相关数据表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "蓝图生产相关数据表")
@Data
@TableName(value = "blueprints_data")
public class BlueprintsDataPO extends BaseEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 产物typeId
     */
    @TableField(value = "type_id")
    @Schema(description = "产物typeId")
    private Long typeId;

    /**
     * 产物名称
     */
    @TableField(value = "type_name")
    @Schema(description = "产物名称")
    private String typeName;

    /**
     * 蓝图typeId
     */
    @TableField(value = "blueprint_type_id")
    @Schema(description = "蓝图typeId")
    private Long blueprintTypeId;

    /**
     * 蓝图名称
     */
    @TableField(value = "blueprint_name")
    @Schema(description = "蓝图名称")
    private String blueprintName;

    /**
     * 生产类型
     */
    @TableField(value = "activity_id")
    @Schema(description = "生产类型")
    private Integer activityId;

    /**
     * 单流程产量
     */
    @TableField(value = "quantity")
    @Schema(description = "单流程产量")
    private Integer quantity;

    /**
     * 基础成功率
     */
    @TableField(value = "probability")
    @Schema(description = "基础成功率")
    private Double probability;

    /**
     * 生产基础时间
     */
    @TableField(value = "`time`")
    @Schema(description = "生产基础时间")
    private Integer time;

    /**
     * 最大单次产量
     */
    @TableField(value = "max_production_limit")
    @Schema(description = "最大单次产量")
    private Integer maxProductionLimit;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_TYPE_ID = "type_id";

    public static final String COL_TYPE_NAME = "type_name";

    public static final String COL_BLUEPRINT_TYPE_ID = "blueprint_type_id";

    public static final String COL_BLUEPRINT_NAME = "blueprint_name";

    public static final String COL_ACTIVITY_ID = "activity_id";

    public static final String COL_QUANTITY = "quantity";

    public static final String COL_PROBABILITY = "probability";

    public static final String COL_TIME = "time";

    public static final String COL_MAX_PRODUCTION_LIMIT = "max_production_limit";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
} 