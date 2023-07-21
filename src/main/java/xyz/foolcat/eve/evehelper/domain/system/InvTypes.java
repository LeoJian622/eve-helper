package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
    * 物品表
    */
@Schema(title="物品表")
@Data
@TableName(value = "inv_types")
public class InvTypes  implements Serializable {
    @TableId(value = "type_id", type = IdType.AUTO)
    @Schema(name="")
    private Integer typeId;

    @TableField(value = "group_id")
    @Schema(name="")
    private Integer groupId;

    @TableField(value = "type_name")
    @Schema(name="")
    private String typeName;

    @TableField(value = "description")
    @Schema(name="")
    private String description;

    /**
     * 质量
     */
    @TableField(value = "mass")
    @Schema(name="")
    private Double mass;

    /**
     * 体积
     */
    @TableField(value = "volume")
    @Schema(name="")
    private Double volume;

    /**
     * 打包体积
     */
    @TableField(value = "packaged_volume")
    @Schema(name="")
    private Double packagedVolume;

    /**
     * 容量
     */
    @TableField(value = "capacity")
    @Schema(name="")
    private Double capacity;

    /**
     * 单位数量
     */
    @TableField(value = "portion_size")
    @Schema(name="")
    private Integer portionSize;

    /**
     * 势力ID
     */
    @TableField(value = "faction_id")
    @Schema(name="")
    private Integer factionId;

    /**
     * 种族ID
     */
    @TableField(value = "race_id")
    @Schema(name="")
    private Integer raceId;

    /**
     * 基准价格
     */
    @TableField(value = "base_price")
    @Schema(name="")
    private Double basePrice;

    @TableField(value = "published")
    @Schema(name="")
    private Byte published;

    /**
     * 市场组ID
     */
    @TableField(value = "market_group_id")
    @Schema(name="")
    private Integer marketGroupId;

    /**
     * 模型ID
     */
    @TableField(value = "graphic_id")
    @Schema(name="")
    private Integer graphicId;

    /**
     * 半径
     */
    @TableField(value = "radius")
    @Schema(name="")
    private Double radius;

    /**
     * 图标ID
     */
    @TableField(value = "icon_id")
    @Schema(name="")
    private Integer iconId;

    @TableField(value = "sound_id")
    @Schema(name="")
    private Integer soundId;

    @TableField(value = "sof_faction_name")
    @Schema(name="")
    private String sofFactionName;

    @TableField(value = "sof_material_set_id")
    @Schema(name="")
    private Integer sofMaterialSetId;

    @TableField(value = "meta_group_id")
    @Schema(name="")
    private Integer metaGroupId;

    @TableField(value = "variationparent_type_id")
    @Schema(name="")
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