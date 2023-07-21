package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(title="挖矿明细")
@Data
@TableName(value = "mining_detail")
public class MiningDetail implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name="")
    private String id;

    /**
     * 角色ID
     */
    @TableField(value = "character_id")
    @Schema(name="角色ID")
    private Integer characterId;

    @TableField(value = "character_name")
    @Schema(name="")
    private String characterName;

    /**
     * 开采时该角色所属公司
     */
    @TableField(value = "recorded_corporation_id")
    @Schema(name="开采时该角色所属公司")
    private Integer recordedCorporationId;

    @TableField(value = "recorded_corporation_name")
    @Schema(name="")
    private String recordedCorporationName;

    /**
     * 物品类型ID
     */
    @TableField(value = "type_id")
    @Schema(name="物品类型ID")
    private Integer typeId;

    /**
     * 开采数量
     */
    @TableField(value = "quantity")
    @Schema(name="开采数量")
    private Long quantity;

    /**
     * observer id
     */
    @TableField(value = "observer_id")
    @Schema(name="observer id")
    private Long observerId;

    /**
     * 上次更新时间
     */
    @TableField(value = "last_updated")
    @Schema(name="上次更新时间")
    private Date lastUpdated;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_CHARACTER_ID = "character_id";

    public static final String COL_CHARACTER_NAME = "character_name";

    public static final String COL_RECORDED_CORPORATION_ID = "recorded_corporation_id";

    public static final String COL_RECORDED_CORPORATION_NAME = "recorded_corporation_name";

    public static final String COL_TYPE_ID = "type_id";

    public static final String COL_QUANTITY = "quantity";

    public static final String COL_OBSERVER_ID = "observer_id";

    public static final String COL_LAST_UPDATED = "last_updated";
}