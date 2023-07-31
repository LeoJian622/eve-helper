package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 市场组
 */
@Schema(description = "市场组")
@Data
@TableName(value = "market_groups")
public class MarketGroups implements Serializable {
    @TableField(value = "market_group_id")
    @Schema(description = "")
    private Integer marketGroupId;

    @TableField(value = "description_id")
    @Schema(description = "")
    private String descriptionId;

    @TableField(value = "has_types")
    @Schema(description = "")
    private Byte hasTypes;

    @TableField(value = "icon_id")
    @Schema(description = "")
    private Integer iconId;

    @TableField(value = "name_id")
    @Schema(description = "")
    private String nameId;

    @TableField(value = "parent_group_id")
    @Schema(description = "")
    private Integer parentGroupId;

    private static final long serialVersionUID = 1L;

    public static final String COL_MARKET_GROUP_ID = "market_group_id";

    public static final String COL_DESCRIPTION_ID = "description_id";

    public static final String COL_HAS_TYPES = "has_types";

    public static final String COL_ICON_ID = "icon_id";

    public static final String COL_NAME_ID = "name_id";

    public static final String COL_PARENT_GROUP_ID = "parent_group_id";
}