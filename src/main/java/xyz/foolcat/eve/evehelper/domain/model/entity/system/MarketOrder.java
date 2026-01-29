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

    public static final String COL_ORDER_ID = "order_id";

    public static final String COL_DURATION = "duration";

    public static final String COL_IS_BUY_ORDER = "is_buy_order";

    public static final String COL_ISSUED = "issued";

    public static final String COL_LOCATION_ID = "location_id";

    public static final String COL_MIN_VOLUME = "min_volume";

    public static final String COL_PRICE = "price";

    public static final String COL_ORDER_RANGE = "order_range";

    public static final String COL_SYSTEM_ID = "system_id";

    public static final String COL_TYPE_ID = "type_id";

    public static final String COL_VOLUME_REMAIN = "volume_remain";

    public static final String COL_VOLUME_TOTAL = "volume_total";

    public static final String COL_REGION_ID = "region_id";
} 