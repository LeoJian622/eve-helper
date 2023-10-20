package xyz.foolcat.eve.evehelper.dto.esi;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * 制造作业ESI接收DTO
 * @author Leojan
 * @date 2022-04-15 16:23
 */

@Schema(title="制造作业表")
@Data
@EqualsAndHashCode(callSuper=true)
public class IndustryJobDTO extends BaseEntity implements Serializable {
    /**
     * 作业ID
     */
    @Schema(name="作业ID")
    private String jobId;

    /**
     * 蓝图名称
     */
    @Schema(name="蓝图ID")
    private String blueprintId;

    /**
     * 蓝图类型
     */
    @Schema(name="蓝图类型ID")
    private String blueprintTypeId;

    /**
     * 花费
     */
    @Schema(name="花费")
    private String cost;

    /**
     * 可用流程数
     */
    @Schema(name="可用流程数")
    private String licensedRuns;

    /**
     * 发明成功几率
     */
    @Schema(name="发明成功几率")
    private String probability;

    /**
     * 建筑ID
     */
    @Schema(name="建筑ID")
    private String stationId;

    /**
     * 产出存放ID
     */
    @Schema(name="产出存放ID")
    private String outputLocationId;

    /**
     * 产出物品类型
     */
    @Schema(name="产出物品类型")
    private String productTypeId;

    /**
     * 蓝图位置ID
     */
    @Schema(name="蓝图位置ID")
    private String blueprintLocationId;

    /**
     * 作业时间
     */
    @Schema(name="作业时间")
    private String duration;

    /**
     * 启动人物
     */
    @Schema(name="启动人物")
    private String installerId;

    /**
     * 完成人物
     */
    @Schema(name="完成人物")
    private String completedCharacterId;

    /**
     * 活动ID
     */
    @Schema(name="活动ID")
    private String activityId;

    /**
     * 作业设施ID
     */
    @Schema(name="作业设施ID")
    private String facilityId;

    /**
     * 作业次数
     */
    @Schema(name="作业次数")
    private String runs;

    /**
     * 开始日期
     */
    @Schema(name="开始日期")
    private Date startDate;

    /**
     * 开始日期
     */
    @Schema(name="暂停日期")
    private Date pauseDate;

    /**
     * 结束日期
     */
    @Schema(name="结束日期")
    private Date endDate;

    /**
     * active-活动 cancelled-取消 delivered-交付 paused-暂停 ready-准备好 reverted-恢复
     */
    @TableField(value = "`status`")
    @Schema(name="active-活动 cancelled-取消 delivered-交付 paused-暂停 ready-准备好 reverted-恢复")
    private String status;

    public static final String COL_JOB_ID = "job_id";

    public static final String COL_BLUEPRINT = "blueprint";

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
}