package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
/**
 * 采矿详情表
 * @author Leojan
 */
@Schema(description = "采矿详情表")
@Data
@TableName(value = "mining_detail")
public class MiningDetailPO  implements Serializable {
    @TableId(value = "id")
    @Schema(description = "")
    private String id;
    /**
     * 人物ID
     */
    @TableField(value = "character_id")
    @Schema(description = "人物ID")
    private Integer characterId;
    @TableField(value = "character_name")
    @Schema(description = "")
    private String characterName;
    /**
     * 开采时该人物所属公司
     */
    @TableField(value = "recorded_corporation_id")
    @Schema(description = "开采时该人物所属公司")
    private Integer recordedCorporationId;
    @TableField(value = "recorded_corporation_name")
    @Schema(description = "")
    private String recordedCorporationName;
    /**
     * 物品类型ID
     */
    @TableField(value = "type_id")
    @Schema(description = "物品类型ID")
    private Integer typeId;
    /**
     * 开采数量
     */
    @TableField(value = "quantity")
    @Schema(description = "开采数量")
    private Long quantity;
    /**
     * observer id
     */
    @TableField(value = "observer_id")
    @Schema(description = "observer id")
    private Long observerId;
    /**
     * 上次更新时间
     */
    @TableField(value = "last_updated")
    @Schema(description = "上次更新时间")
    private Date lastUpdated;
    @Serial
    private static final long serialVersionUID = 1L;
} 