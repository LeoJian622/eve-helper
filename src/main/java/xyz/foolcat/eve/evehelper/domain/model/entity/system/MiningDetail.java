package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 采矿详情表
 * @author Leojan
 */
@Data
public class MiningDetail  implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 人物ID
     */
    private Long characterId;

    /**
     * 人物名称
     */
    private String characterName;

    /**
     * 开采时该人物所属公司
     */
    private Long recordedCorporationId;

    /**
     * 公司名称
     */
    private String recordedCorporationName;

    /**
     * 物品类型ID
     */
    private Long typeId;

    /**
     * 开采数量
     */
    private Long quantity;

    /**
     * 观察者ID
     */
    private Long observerId;

    /**
     * 上次更新时间
     */
    private Date lastUpdated;

    private static final long serialVersionUID = 1L;
} 