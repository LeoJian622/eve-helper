package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 工业工作实体
 * @author Leojan
 */
@Data
public class IndustryJob implements Serializable {

    /**
     * 作业ID
     */
    private Long jobId;

    /**
     * 工业的活动ID
     */
    private Integer activityId;

    /**
     * 活动类型
     */
    private String activity;

    /**
     * 蓝图itemID
     */
    private Long blueprintId;

    /**
     * 蓝图invType ID
     */
    private Integer blueprintTypeId;

    /**
     * 蓝图名称
     */
    private String blueprintType;

    /**
     * 产物类型ID
     */
    private Integer productTypeId;

    /**
     * 产出物品类型
     */
    private String productType;

    /**
     * 花费
     */
    private Integer cost;

    /**
     * 作业时间
     */
    private Long duration;

    /**
     * 作业次数
     */
    private Integer runs;

    /**
     * 可用流程数
     */
    private Integer licensedRuns;

    /**
     * 发明成功几率
     */
    private Long probability;

    /**
     * 建筑ID
     */
    private Long stationId;

    /**
     * 产出存放ID
     */
    private Long outputLocationId;

    /**
     * 蓝图位置ID
     */
    private Long blueprintLocationId;

    /**
     * 启动角色
     */
    private String installer;

    /**
     * 完成角色
     */
    private String completedCharacter;

    /**
     * 作业设施ID
     */
    private Long facilityId;

    /**
     * 开始日期
     */
    private OffsetDateTime startDate;

    /**
     * 结束日期
     */
    private OffsetDateTime endDate;

    /**
     * 完成时间
     */
    private OffsetDateTime completedDate;

    /**
     * active-活动 cancelled-取消 delivered-交付 paused-暂停 ready-准备好 reverted-恢复
     */
    private String status;

    /**
     * 该建筑服务下线时间
     */
    private OffsetDateTime pauseDate;

    /**
     * 启动角色ID
     */
    private Long installerId;

    /**
     * 公司ID
     */
    private Integer corporationId;

    private static final long serialVersionUID = 1L;


    public static final String STATUS_ACTIVE = "active";

    public static final String STATUS_CANCELLED = "cancelled";

    public static final String STATUS_DELIVERED = "delivered";

    public static final String STATUS_PAUSED = "paused";

    public static final String STATUS_READY = "ready";

    public static final String STATUS_REVERTED = "reverted";
} 