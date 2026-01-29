package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 建筑表
 * @author Leojan
 */
@Data
public class Structure  implements Serializable {

    /**
     * 建筑ID
     */
    private Long structureId;

    /**
     * 公司ID
     */
    private Long corporationId;

    /**
     * 燃料耗尽时间
     */
    private OffsetDateTime fuelExpires;

    /**
     * 建筑名称
     */
    private String name;

    /**
     * 下次增强时间
     */
    private OffsetDateTime nextReinforceApply;

    /**
     * 下次增强小时
     */
    private Integer nextReinforceHour;

    /**
     * 方案ID
     */
    private Long profileId;

    /**
     * 增强时间
     */
    private Integer reinforceHour;

    /**
     * 状态
     */
    private String state;

    /**
     * 状态结束时间
     */
    private OffsetDateTime stateTimerEnd;

    /**
     * 状态开始时间
     */
    private OffsetDateTime stateTimerStart;

    /**
     * 星系ID
     */
    private Long systemId;

    /**
     * 物品类型ID
     */
    private Long typeId;

    /**
     * 解锚时间
     */
    private OffsetDateTime unanchorsAt;

    /**
     * 建筑服务
     */
    private String services;

    private static final long serialVersionUID = 1L;
} 