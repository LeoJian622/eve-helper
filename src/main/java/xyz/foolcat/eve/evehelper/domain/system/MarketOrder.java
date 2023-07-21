package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(title="市场订单")
@Data
@TableName(value = "market_order")
public class MarketOrder implements Serializable {
    /**
     * 订单编号
     */
    @TableId(value = "order_id")
    @Schema(name="订单编号")
    private Long orderId;

    /**
     * 持续时间
     */
    @TableField(value = "duration")
    @Schema(name="持续时间")
    private Long duration;

    /**
     * 是否买单
     */
    @TableField(value = "is_buy_order")
    @Schema(name="是否买单")
    private Boolean isBuyOrder;

    /**
     * 发布时间
     */
    @TableField(value = "issued")
    @Schema(name="发布时间")
    private Date issued;

    /**
     * 建筑ID
     */
    @TableField(value = "location_id")
    @Schema(name="建筑ID")
    private Long locationId;

    /**
     * 最小交易数量
     */
    @TableField(value = "min_volume")
    @Schema(name="最小交易数量")
    private Long minVolume;

    /**
     * 价格
     */
    @TableField(value = "price")
    @Schema(name="价格")
    private Long price;

    /**
     * 订单范围
     */
    @TableField(value = "order_range")
    @Schema(name="订单范围")
    private String orderRange;

    /**
     * 星系ID
     */
    @TableField(value = "system_id")
    @Schema(name="星系ID")
    private Long systemId;

    /**
     * 物品类型ID
     */
    @TableField(value = "type_id")
    @Schema(name="物品类型ID")
    private Long typeId;

    /**
     * 剩余数量
     */
    @TableField(value = "volume_remain")
    @Schema(name="剩余数量")
    private Long volumeRemain;

    /**
     * 总数量
     */
    @TableField(value = "volume_total")
    @Schema(name="总数量")
    private Long volumeTotal;

    /**
     * 星域ID
     */
    @TableField(value = "region_id")
    @Schema(name="星域ID")
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