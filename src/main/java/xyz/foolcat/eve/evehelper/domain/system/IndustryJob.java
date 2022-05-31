package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
 * 制造作业表
 */
@ApiModel(value = "制造作业表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "industry_job")
public class IndustryJob extends BaseEntity implements Serializable {
    /**
     * 作业ID
     */
    @TableId(value = "job_id", type = IdType.AUTO)
    @ApiModelProperty(value = "作业ID")
    private Long jobId;

    /**
     * 蓝图itemID
     */
    @TableField(value = "blueprint_id")
    @ApiModelProperty(value = "蓝图itemID")
    private Long blueprintId;

    /**
     * 蓝图invType ID
     */
    @TableField(value = "blueprint_type_id")
    @ApiModelProperty(value = "蓝图invType ID")
    private Integer blueprintTypeId;

    /**
     * 蓝图名称
     */
    @TableField(value = "blueprint_type")
    @ApiModelProperty(value = "蓝图名称")
    private String blueprintType;

    /**
     * 花费
     */
    @TableField(value = "cost")
    @ApiModelProperty(value = "花费")
    private Integer cost;

    /**
     * 可用流程数
     */
    @TableField(value = "licensed_runs")
    @ApiModelProperty(value = "可用流程数")
    private Integer licensedRuns;

    /**
     * 发明成功几率
     */
    @TableField(value = "probability")
    @ApiModelProperty(value = "发明成功几率")
    private Long probability;

    /**
     * 建筑ID
     */
    @TableField(value = "station_id")
    @ApiModelProperty(value = "建筑ID")
    private Long stationId;

    /**
     * 产出存放ID
     */
    @TableField(value = "output_location_id")
    @ApiModelProperty(value = "产出存放ID")
    private Long outputLocationId;

    /**
     * 产出物品类型
     */
    @TableField(value = "product_type")
    @ApiModelProperty(value = "产出物品类型")
    private String productType;

    /**
     * 蓝图位置ID
     */
    @TableField(value = "blueprint_location_id")
    @ApiModelProperty(value = "蓝图位置ID")
    private Long blueprintLocationId;

    /**
     * 作业时间
     */
    @TableField(value = "duration")
    @ApiModelProperty(value = "作业时间")
    private Long duration;

    /**
     * 启动角色
     */
    @TableField(value = "installer")
    @ApiModelProperty(value = "启动角色")
    private String installer;

    /**
     * 完成角色
     */
    @TableField(value = "completed_character")
    @ApiModelProperty(value = "完成角色")
    private String completedCharacter;

    /**
     * 活动ID
     */
    @TableField(value = "activity")
    @ApiModelProperty(value = "活动ID")
    private Long activity;

    /**
     * 作业设施ID
     */
    @TableField(value = "facility_id")
    @ApiModelProperty(value = "作业设施ID")
    private Long facilityId;

    /**
     * 作业次数
     */
    @TableField(value = "runs")
    @ApiModelProperty(value = "作业次数")
    private Integer runs;

    /**
     * 开始日期
     */
    @TableField(value = "start_date")
    @ApiModelProperty(value = "开始日期")
    private Date startDate;

    /**
     * 结束日期
     */
    @TableField(value = "end_date")
    @ApiModelProperty(value = "结束日期")
    private Date endDate;

    /**
     * active-活动 cancelled-取消 delivered-交付 paused-暂停 ready-准备好 reverted-恢复
     */
    @TableField(value = "`status`")
    @ApiModelProperty(value = "active-活动 cancelled-取消 delivered-交付 paused-暂停 ready-准备好 reverted-恢复")
    private String status;

    /**
     * 该建筑服务下线时间
     */
    @TableField(value = "pause_date")
    @ApiModelProperty(value = "该建筑服务下线时间")
    private Date pauseDate;

    /**
     * 启动角色ID
     */
    @TableField(value = "installer_id")
    @ApiModelProperty(value = "启动角色ID")
    private Long installerId;

    public static final String COL_JOB_ID = "job_id";

    public static final String COL_BLUEPRINT_ID = "blueprint_id";

    public static final String COL_BLUEPRINT_TYPE_ID = "blueprint_type_id";

    public static final String COL_BLUEPRINT_TYPE = "blueprint_type";

    public static final String COL_COST = "cost";

    public static final String COL_LICENSED_RUNS = "licensed_runs";

    public static final String COL_PROBABILITY = "probability";

    public static final String COL_STATION_ID = "station_id";

    public static final String COL_OUTPUT_LOCATION_ID = "output_location_id";

    public static final String COL_PRODUCT_TYPE = "product_type";

    public static final String COL_BLUEPRINT_LOCATION_ID = "blueprint_location_id";

    public static final String COL_DURATION = "duration";

    public static final String COL_INSTALLER = "installer";

    public static final String COL_COMPLETED_CHARACTER = "completed_character";

    public static final String COL_ACTIVITY = "activity";

    public static final String COL_FACILITY_ID = "facility_id";

    public static final String COL_RUNS = "runs";

    public static final String COL_START_DATE = "start_date";

    public static final String COL_END_DATE = "end_date";

    public static final String COL_STATUS = "status";

    public static final String COL_PAUSE_DATE = "pause_date";

    public static final String COL_INSTALLER_ID = "installer_id";
}