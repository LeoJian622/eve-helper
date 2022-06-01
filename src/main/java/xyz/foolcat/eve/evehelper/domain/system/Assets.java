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

@ApiModel(value = "assets")
@Data
@TableName(value = "assets")
public class Assets  implements Serializable {
    /**
     * 物品ID
     */
    @TableId(value = "item_id")
    @ApiModelProperty(value = "物品ID")
    private Long itemId;

    /**
     * invTypeID
     */
    @TableField(value = "type_id")
    @ApiModelProperty(value = "invTypeID")
    private Integer typeId;

    /**
     * 建筑ID
     */
    @TableField(value = "location_id")
    @ApiModelProperty(value = "建筑ID")
    private Long locationId;

    /**
     * 类型station, solar_system, item, other
     */
    @TableField(value = "location_type")
    @ApiModelProperty(value = "类型station, solar_system, item, other")
    private String locationType;

    @TableField(value = "location_flag")
    @ApiModelProperty(value = "")
    private String locationFlag;

    @TableField(value = "is_singleton")
    @ApiModelProperty(value = "")
    private Boolean isSingleton;

    /**
     * 蓝图拷贝
     */
    @TableField(value = "is_blueprint_copy")
    @ApiModelProperty(value = "蓝图拷贝")
    private Boolean isBlueprintCopy;

    /**
     * 数量
     */
    @TableField(value = "quantity")
    @ApiModelProperty(value = "数量")
    private Long quantity;

    /**
     * 所有者ID
     */
    @TableField(value = "owner_id")
    @ApiModelProperty(value = "所有者ID")
    private Long ownerId;

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