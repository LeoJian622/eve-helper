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
    * 物品表
    */
@ApiModel(value="物品表")
@Data
@TableName(value = "inv_types")
public class InvTypes  implements Serializable {
    @TableId(value = "type_id", type = IdType.AUTO)
    @ApiModelProperty(value="")
    private Integer typeId;

    @TableField(value = "group_id")
    @ApiModelProperty(value="")
    private Integer groupId;

    @TableField(value = "type_name")
    @ApiModelProperty(value="")
    private String typeName;

    @TableField(value = "description")
    @ApiModelProperty(value="")
    private String description;

    /**
     * 质量
     */
    @TableField(value = "mass")
    @ApiModelProperty(value="")
    private Double mass;

    /**
     * 体积
     */
    @TableField(value = "volume")
    @ApiModelProperty(value="")
    private Double volume;

    /**
     * 打包体积
     */
    @TableField(value = "packaged_volume")
    @ApiModelProperty(value="")
    private Double packagedVolume;

    /**
     * 容量
     */
    @TableField(value = "capacity")
    @ApiModelProperty(value="")
    private Double capacity;

    /**
     * 单位数量
     */
    @TableField(value = "portion_size")
    @ApiModelProperty(value="")
    private Integer portionSize;

    /**
     * 势力ID
     */
    @TableField(value = "faction_id")
    @ApiModelProperty(value="")
    private Integer factionId;

    /**
     * 种族ID
     */
    @TableField(value = "race_id")
    @ApiModelProperty(value="")
    private Integer raceId;

    /**
     * 基准价格
     */
    @TableField(value = "base_price")
    @ApiModelProperty(value="")
    private Double basePrice;

    @TableField(value = "published")
    @ApiModelProperty(value="")
    private Byte published;

    /**
     * 市场组ID
     */
    @TableField(value = "market_group_id")
    @ApiModelProperty(value="")
    private Integer marketGroupId;

    /**
     * 模型ID
     */
    @TableField(value = "graphic_id")
    @ApiModelProperty(value="")
    private Integer graphicId;

    /**
     * 半径
     */
    @TableField(value = "radius")
    @ApiModelProperty(value="")
    private Double radius;

    /**
     * 图标ID
     */
    @TableField(value = "icon_id")
    @ApiModelProperty(value="")
    private Integer iconId;

    @TableField(value = "sound_id")
    @ApiModelProperty(value="")
    private Integer soundId;

    @TableField(value = "sof_faction_name")
    @ApiModelProperty(value="")
    private String sofFactionName;

    @TableField(value = "sof_material_set_id")
    @ApiModelProperty(value="")
    private Integer sofMaterialSetId;

    @TableField(value = "meta_group_id")
    @ApiModelProperty(value="")
    private Integer metaGroupId;

    @TableField(value = "variationparent_type_id")
    @ApiModelProperty(value="")
    private Integer variationparentTypeId;

    public static final String COL_TYPE_ID = "type_id";

    public static final String COL_GROUP_ID = "group_id";

    public static final String COL_TYPE_NAME = "type_name";

    public static final String COL_DESCRIPTION = "description";

    public static final String COL_MASS = "mass";

    public static final String COL_VOLUME = "volume";

    public static final String COL_PACKAGED_VOLUME = "packaged_volume";

    public static final String COL_CAPACITY = "capacity";

    public static final String COL_PORTION_SIZE = "portion_size";

    public static final String COL_FACTION_ID = "faction_id";

    public static final String COL_RACE_ID = "race_id";

    public static final String COL_BASE_PRICE = "base_price";

    public static final String COL_PUBLISHED = "published";

    public static final String COL_MARKET_GROUP_ID = "market_group_id";

    public static final String COL_GRAPHIC_ID = "graphic_id";

    public static final String COL_RADIUS = "radius";

    public static final String COL_ICON_ID = "icon_id";

    public static final String COL_SOUND_ID = "sound_id";

    public static final String COL_SOF_FACTION_NAME = "sof_faction_name";

    public static final String COL_SOF_MATERIAL_SET_ID = "sof_material_set_id";

    public static final String COL_META_GROUP_ID = "meta_group_id";

    public static final String COL_VARIATIONPARENT_TYPE_ID = "variationparent_type_id";
}