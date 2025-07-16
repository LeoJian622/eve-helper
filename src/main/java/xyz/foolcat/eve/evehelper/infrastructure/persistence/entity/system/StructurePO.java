package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑表
 * @author Leojan
 */
@Schema(description = "建筑表")
@Data
@TableName(value = "`structure`")
public class StructurePO  implements Serializable {

    /**
     * 建筑ID
     */
    @TableField(value = "structure_id")
    @Schema(description="建筑ID")
    private Long structureId;

    /**
     * 公司ID
     */
    @TableField(value = "corporation_id")
    @Schema(description="公司ID")
    private Long corporationId;

    /**
     * 燃料耗尽时间
     */
    @TableField(value = "fuel_expires")
    @Schema(description="燃料耗尽时间")
    private OffsetDateTime fuelExpires;

    /**
     * 建筑名称
     */
    @TableField(value = "`name`")
    @Schema(description="建筑名称")
    private String name;

    /**
     * 下次增强时间
     */
    @TableField(value = "next_reinforce_apply")
    @Schema(description="下次增强时间")
    private OffsetDateTime nextReinforceApply;

    /**
     * 下次增强小时
     */
    @TableField(value = "next_reinforce_hour")
    @Schema(description="下次增强小时")
    private Integer nextReinforceHour;

    /**
     * 方案ID
     */
    @TableField(value = "profile_id")
    @Schema(description="方案ID")
    private Long profileId;

    /**
     * 增强时间
     */
    @TableField(value = "reinforce_hour")
    @Schema(description="增强时间")
    private Integer reinforceHour;

    /**
     * 状态
     */
    @TableField(value = "`state`")
    @Schema(description="状态")
    private String state;

    /**
     * 状态结束时间
     */
    @TableField(value = "state_timer_end")
    @Schema(description="状态结束时间")
    private OffsetDateTime stateTimerEnd;

    /**
     * 状态开始时间
     */
    @TableField(value = "state_timer_start")
    @Schema(description="状态开始时间")
    private OffsetDateTime stateTimerStart;

    /**
     * 星系ID
     */
    @TableField(value = "system_id")
    @Schema(description="星系ID")
    private Long systemId;

    /**
     * 物品类型ID
     */
    @TableField(value = "type_id")
    @Schema(description="物品类型ID")
    private Long typeId;

    /**
     * 解锚时间
     */
    @TableField(value = "unanchors_at")
    @Schema(description="解锚时间")
    private OffsetDateTime unanchorsAt;

    /**
     * 建筑服务
     */
    @TableField(value = "services")
    @Schema(description="建筑服务")
    private String services;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_STRUCTURE_ID = "structure_id";

    public static final String COL_CORPORATION_ID = "corporation_id";

    public static final String COL_FUEL_EXPIRES = "fuel_expires";

    public static final String COL_NAME = "name";

    public static final String COL_NEXT_REINFORCE_APPLY = "next_reinforce_apply";

    public static final String COL_NEXT_REINFORCE_HOUR = "next_reinforce_hour";

    public static final String COL_PROFILE_ID = "profile_id";

    public static final String COL_REINFORCE_HOUR = "reinforce_hour";

    public static final String COL_STATE = "state";

    public static final String COL_STATE_TIMER_END = "state_timer_end";

    public static final String COL_STATE_TIMER_START = "state_timer_start";

    public static final String COL_SYSTEM_ID = "system_id";

    public static final String COL_TYPE_ID = "type_id";

    public static final String COL_UNANCHORS_AT = "unanchors_at";

    public static final String COL_SERVICES = "services";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
} 