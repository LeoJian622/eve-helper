package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 市场订单实体
 * @author Leojan
 */
@Data
public class MarketOrder implements Serializable {
    /**
     * 订单编号
     */
    private Long orderId;

    /**
     * 持续时间
     */
    private Long duration;

    /**
     * 是否买单
     */
    private Boolean isBuyOrder;

    /**
     * 发布时间
     */
    private Date issued;

    /**
     * 建筑ID
     */
    private Long locationId;

    /**
     * 最小交易数量
     */
    private Long minVolume;

    /**
     * 价格
     */
    private Long price;

    /**
     * 订单范围
     */
    private String orderRange;

    /**
     * 星系ID
     */
    private Long systemId;

    /**
     * 物品类型ID
     */
    private Long typeId;

    /**
     * 剩余数量
     */
    private Long volumeRemain;

    /**
     * 总数量
     */
    private Long volumeTotal;

    /**
     * 星域ID
     */
    private Long regionId;

    private static final long serialVersionUID = 1L;
} 