package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
@TableName(value = "assets")
public class Asserts implements Serializable {
    /**
     * 物品ID
     */
    @TableId(value = "item_id")
    @Schema(description = "物品ID")
    private Long itemId;

    /**
     * invTypeID
     */
    @TableField(value = "type_id")
    @Schema(description = "invTypeID")
    private Integer typeId;

    /**
     * 建筑ID
     */
    @TableField(value = "location_id")
    @Schema(description = "建筑ID")
    private Long locationId;

    /**
     * 类型station, solar_system, item, other
     */
    @TableField(value = "location_type")
    @Schema(description = "类型station, solar_system, item, other")
    private String locationType;

    @TableField(value = "location_flag")
    @Schema(description = "")
    private String locationFlag;

    @TableField(value = "is_singleton")
    @Schema(description = "")
    private Boolean isSingleton;

    /**
     * 蓝图拷贝
     */
    @TableField(value = "is_blueprint_copy")
    @Schema(description = "蓝图拷贝")
    private Boolean isBlueprintCopy;

    /**
     * 数量
     */
    @TableField(value = "quantity")
    @Schema(description = "数量")
    private Long quantity;

    /**
     * 所有者ID
     */
    @TableField(value = "owner_id")
    @Schema(description = "所有者ID")
    private Long ownerId;

    private static final long serialVersionUID = 1L;

    public static final String COL_ITEM_ID = "item_id";

    public static final String COL_TYPE_ID = "type_id";

    public static final String COL_LOCATION_ID = "location_id";

    public static final String COL_LOCATION_TYPE = "location_type";

    public static final String COL_LOCATION_FLAG = "location_flag";

    public static final String COL_IS_SINGLETON = "is_singleton";

    public static final String COL_IS_BLUEPRINT_COPY = "is_blueprint_copy";

    public static final String COL_QUANTITY = "quantity";

    public static final String COL_OWNER_ID = "owner_id";
}