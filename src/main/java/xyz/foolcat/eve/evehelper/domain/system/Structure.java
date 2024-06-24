package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Schema
@Data
@TableName(value = "`structure`")
public class Structure implements Serializable {
    /**
     * 建筑ID
     */
    @TableId(value = "structure_id", type = IdType.AUTO)
    @Schema(description="建筑ID")
    private Long structureId;

    /**
     * 公司ID
     */
    @TableField(value = "corporation_id")
    @Schema(description="公司ID")
    private Integer corporationId;

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
    private Integer profileId;

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
    private Integer systemId;

    /**
     * 物品类型ID
     */
    @TableField(value = "type_id")
    @Schema(description="物品类型ID")
    private Integer typeId;

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
}