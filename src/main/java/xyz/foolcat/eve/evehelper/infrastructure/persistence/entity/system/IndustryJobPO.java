package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

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
@TableName(value = "industry_job")
public class IndustryJobPO implements Serializable {
    /**
     * 作业ID
     */
    @TableId(value = "job_id", type = IdType.AUTO)
    @Schema(description = "作业ID")
    private Long jobId;

    /**
     * 工业的活动ID
     */
    @TableField(value = "activity_id")
    @Schema(description = "工业的活动ID")
    private Integer activityId;

    /**
     * 活动类型
     */
    @TableField(value = "activity")
    @Schema(description = "活动类型")
    private String activity;

    /**
     * 蓝图itemID
     */
    @TableField(value = "blueprint_id")
    @Schema(description = "蓝图itemID")
    private Long blueprintId;

    /**
     * 蓝图invType ID
     */
    @TableField(value = "blueprint_type_id")
    @Schema(description = "蓝图invType ID")
    private Integer blueprintTypeId;

    /**
     * 蓝图名称
     */
    @TableField(value = "blueprint_type")
    @Schema(description = "蓝图名称")
    private String blueprintType;

    /**
     * 产物类型ID
     */
    @TableField(value = "product_type_id")
    @Schema(description = "产物类型ID")
    private Integer productTypeId;

    /**
     * 产出物品类型
     */
    @TableField(value = "product_type")
    @Schema(description = "产出物品类型")
    private String productType;

    /**
     * 花费
     */
    @TableField(value = "cost")
    @Schema(description = "花费")
    private Integer cost;

    /**
     * 作业时间
     */
    @TableField(value = "duration")
    @Schema(description = "作业时间")
    private Long duration;

    /**
     * 作业次数
     */
    @TableField(value = "runs")
    @Schema(description = "作业次数")
    private Integer runs;

    /**
     * 可用流程数
     */
    @TableField(value = "licensed_runs")
    @Schema(description = "可用流程数")
    private Integer licensedRuns;

    /**
     * 发明成功几率
     */
    @TableField(value = "probability")
    @Schema(description = "发明成功几率")
    private Long probability;

    /**
     * 建筑ID
     */
    @TableField(value = "station_id")
    @Schema(description = "建筑ID")
    private Long stationId;

    /**
     * 产出存放ID
     */
    @TableField(value = "output_location_id")
    @Schema(description = "产出存放ID")
    private Long outputLocationId;

    /**
     * 蓝图位置ID
     */
    @TableField(value = "blueprint_location_id")
    @Schema(description = "蓝图位置ID")
    private Long blueprintLocationId;

    /**
     * 启动角色
     */
    @TableField(value = "installer")
    @Schema(description = "启动角色")
    private String installer;

    /**
     * 完成角色
     */
    @TableField(value = "completed_character")
    @Schema(description = "完成角色")
    private String completedCharacter;

    /**
     * 作业设施ID
     */
    @TableField(value = "facility_id")
    @Schema(description = "作业设施ID")
    private Long facilityId;

    /**
     * 开始日期
     */
    @TableField(value = "start_date")
    @Schema(description = "开始日期")
    private OffsetDateTime startDate;

    /**
     * 结束日期
     */
    @TableField(value = "end_date")
    @Schema(description = "结束日期")
    private OffsetDateTime endDate;

    /**
     * 完成时间
     */
    @TableField(value = "completed_date")
    @Schema(description = "完成时间")
    private OffsetDateTime completedDate;

    /**
     * active-活动 cancelled-取消 delivered-交付 paused-暂停 ready-准备好 reverted-恢复
     */
    @TableField(value = "`status`")
    @Schema(description = "active-活动 cancelled-取消 delivered-交付 paused-暂停 ready-准备好 reverted-恢复")
    private String status;

    /**
     * 该建筑服务下线时间
     */
    @TableField(value = "pause_date")
    @Schema(description = "该建筑服务下线时间")
    private OffsetDateTime pauseDate;

    /**
     * 启动角色ID
     */
    @TableField(value = "installer_id")
    @Schema(description = "启动角色ID")
    private Long installerId;

    /**
     * 公司ID
     */
    @TableField(value = "corporation_id")
    @Schema(description = "公司ID")
    private Integer corporationId;

    private static final long serialVersionUID = 1L;

    public static final String COL_JOB_ID = "job_id";

    public static final String COL_INSTALLER_ID = "installer_id";

    public static final String COL_INSTALLER_NAME = "installer_name";

    public static final String COL_FACILITY_ID = "facility_id";

    public static final String COL_FACILITY_NAME = "facility_name";

    public static final String COL_BLUEPRINT_ID = "blueprint_id";

    public static final String COL_BLUEPRINT_TYPE_ID = "blueprint_type_id";

    public static final String COL_BLUEPRINT_TYPE_NAME = "blueprint_type_name";

    public static final String COL_BLUEPRINT_LOCATION_ID = "blueprint_location_id";

    public static final String COL_OUTPUT_LOCATION_ID = "output_location_id";

    public static final String COL_RUNS = "runs";

    public static final String COL_COST = "cost";

    public static final String COL_LICENSED_RUNS = "licensed_runs";

    public static final String COL_PROBABILITY = "probability";

    public static final String COL_PRODUCT_TYPE_ID = "product_type_id";

    public static final String COL_PRODUCT_TYPE_NAME = "product_type_name";

    public static final String COL_STATUS = "status";

    public static final String COL_START_DATE = "start_date";

    public static final String COL_END_DATE = "end_date";

    public static final String COL_PAUSE_DATE = "pause_date";

    public static final String COL_COMPLETED_DATE = "completed_date";

    public static final String COL_COMPLETED_CHARACTER_ID = "completed_character_id";

    public static final String COL_SUCCESSFUL_RUNS = "successful_runs";

} 