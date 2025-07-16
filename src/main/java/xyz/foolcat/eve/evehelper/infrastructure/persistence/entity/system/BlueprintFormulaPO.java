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
 * 蓝图材料配方基础数据
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "蓝图材料配方基础数据")
@Data
@TableName(value = "blueprint_formula")
public class BlueprintFormulaPO extends BaseEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 蓝图ID
     */
    @TableField(value = "blueprint_id")
    @Schema(description = "蓝图ID")
    private Long blueprintId;

    /**
     * 原材料typeid
     */
    @TableField(value = "material_type_id")
    @Schema(description = "原材料typeid")
    private Long materialTypeId;

    /**
     * 名称
     */
    @TableField(value = "type_name")
    @Schema(description = "名称")
    private String typeName;

    /**
     * 活动类型
     */
    @TableField(value = "activity_id")
    @Schema(description = "活动类型")
    private Integer activityId;

    /**
     * 需求数量
     */
    @TableField(value = "quantity")
    @Schema(description = "需求数量")
    private Integer quantity;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_BLUEPRINT_ID = "blueprint_id";

    public static final String COL_MATERIAL_TYPE_ID = "material_type_id";

    public static final String COL_TYPE_NAME = "type_name";

    public static final String COL_ACTIVITY_ID = "activity_id";

    public static final String COL_QUANTITY = "quantity";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
} 