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
@Schema(description = "物品表")
@Data
@TableName(value = "inv_types")
public class InvTypes implements Serializable {
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
    @TableId(value = "type_id", type = IdType.AUTO)
    @Schema(description = "物品ID")
    private Integer typeId;

    @TableField(value = "group_id")
    @Schema(description = "物品组ID")
    private Integer groupId;

    @TableField(value = "`name`")
    @Schema(description = "名称")
    private String name;

    @TableField(value = "description")
    @Schema(description = "描述")
    private String description;

    @TableField(value = "mass")
    @Schema(description = "质量")
    private Double mass;

    @TableField(value = "volume")
    @Schema(description = "体积")
    private Double volume;

    @TableField(value = "packaged_volume")
    @Schema(description = "打包体积")
    private Double packagedVolume;

    @TableField(value = "capacity")
    @Schema(description = "类别")
    private Double capacity;

    @TableField(value = "portion_size")
    @Schema(description = "")
    private Integer portionSize;

    @TableField(value = "faction_id")
    @Schema(description = "")
    private Integer factionId;

    @TableField(value = "race_id")
    @Schema(description = "")
    private Integer raceId;

    @TableField(value = "base_price")
    @Schema(description = "基础价格")
    private Double basePrice;

    @TableField(value = "published")
    @Schema(description = "")
    private Byte published;

    @TableField(value = "market_group_id")
    @Schema(description = "市场组ID")
    private Integer marketGroupId;

    @TableField(value = "graphic_id")
    @Schema(description = "图片ID")
    private Integer graphicId;

    @TableField(value = "radius")
    @Schema(description = "")
    private Double radius;

    @TableField(value = "icon_id")
    @Schema(description = "图标ID")
    private Integer iconId;

    @TableField(value = "sound_id")
    @Schema(description = "音乐ID")
    private Integer soundId;

    @TableField(value = "sof_faction_name")
    @Schema(description = "")
    private String sofFactionName;

    @TableField(value = "sof_material_set_id")
    @Schema(description = "")
    private Integer sofMaterialSetId;

    @TableField(value = "meta_group_id")
    @Schema(description = "")
    private Integer metaGroupId;

    @TableField(value = "variationparent_type_id")
    @Schema(description = "")
    private Integer variationparentTypeId;

    private static final long serialVersionUID = 1L;
}