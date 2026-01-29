package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 物品表
 * @author Leojan
 */
@Schema(description = "物品表")
@Data
@TableName(value = "inv_types")
public class InvTypesPO implements Serializable {

    @TableField(value = "type_id")
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
    @Schema(description = "部分大小")
    private Integer portionSize;

    @TableField(value = "faction_id")
    @Schema(description = "派系ID")
    private Integer factionId;

    @TableField(value = "race_id")
    @Schema(description = "种族ID")
    private Integer raceId;

    @TableField(value = "base_price")
    @Schema(description = "基础价格")
    private Double basePrice;

    @TableField(value = "published")
    @Schema(description = "是否发布")
    private Boolean published;

    @TableField(value = "market_group_id")
    @Schema(description = "市场组ID")
    private Integer marketGroupId;

    @TableField(value = "graphic_id")
    @Schema(description = "图片ID")
    private Integer graphicId;

    @TableField(value = "radius")
    @Schema(description = "半径")
    private Double radius;

    @TableField(value = "icon_id")
    @Schema(description = "图标ID")
    private Integer iconId;

    @TableField(value = "sound_id")
    @Schema(description = "音乐ID")
    private Integer soundId;

    @TableField(value = "sof_faction_name")
    @Schema(description = "SOF派系名称")
    private String sofFactionName;

    @TableField(value = "sof_material_set_id")
    @Schema(description = "SOF材料集ID")
    private Integer sofMaterialSetId;

    @TableField(value = "meta_group_id")
    @Schema(description = "元组ID")
    private Integer metaGroupId;

    @TableField(value = "variationparent_type_id")
    @Schema(description = "变体父类型ID")
    private Integer variationparentTypeId;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_TYPE_ID = "type_id";

    public static final String COL_GROUP_ID = "group_id";

    public static final String COL_NAME = "name";

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

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
} 