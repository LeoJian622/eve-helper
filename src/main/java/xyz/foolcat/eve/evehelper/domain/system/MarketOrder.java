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

@ApiModel(value="market_order")
@Data
@TableName(value = "market_order")
public class MarketOrder implements Serializable {
    /**
     * 订单编号
     */
    @TableId(value = "order_id")
    @ApiModelProperty(value="订单编号")
    private Long orderId;

    /**
     * 持续时间
     */
    @TableField(value = "duration")
    @ApiModelProperty(value="持续时间")
    private Long duration;

    /**
     * 是否买单
     */
    @TableField(value = "is_buy_order")
    @ApiModelProperty(value="是否买单")
    private Boolean isBuyOrder;

    /**
     * 发布时间
     */
    @TableField(value = "issued")
    @ApiModelProperty(value="发布时间")
    private Date issued;

    /**
     * 建筑ID
     */
    @TableField(value = "location_id")
    @ApiModelProperty(value="建筑ID")
    private Long locationId;

    /**
     * 最小交易数量
     */
    @TableField(value = "min_volume")
    @ApiModelProperty(value="最小交易数量")
    private Long minVolume;

    /**
     * 价格
     */
    @TableField(value = "price")
    @ApiModelProperty(value="价格")
    private Long price;

    /**
     * 订单范围
     */
    @TableField(value = "order_range")
    @ApiModelProperty(value="订单范围")
    private String orderRange;

    /**
     * 星系ID
     */
    @TableField(value = "system_id")
    @ApiModelProperty(value="星系ID")
    private Long systemId;

    /**
     * 物品类型ID
     */
    @TableField(value = "type_id")
    @ApiModelProperty(value="物品类型ID")
    private Long typeId;

    /**
     * 剩余数量
     */
    @TableField(value = "volume_remain")
    @ApiModelProperty(value="剩余数量")
    private Long volumeRemain;

    /**
     * 总数量
     */
    @TableField(value = "volume_total")
    @ApiModelProperty(value="总数量")
    private Long volumeTotal;

    /**
     * 星域ID
     */
    @TableField(value = "region_id")
    @ApiModelProperty(value="星域ID")
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