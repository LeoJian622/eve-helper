package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 市场订单
 *
 * @author Leojan
 * date 2023-11-03 14:55
 */

@Data
@Tag(name = "市场订单 200 ok")
public class MarketOrderResponse {

    private Integer duration;

    private Double escrow;

    @JsonProperty("is_buy_order")
    private Boolean isBuyOrder;

    @JsonProperty("is_corporation")
    private Boolean isCorporation;

    private OffsetDateTime issued;

    @JsonProperty("issued_by")
    private Integer issuedBy;

    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("min_volume")
    private Integer minVolume;

    @JsonProperty("order_id")
    private Long orderId;

    private Double price;

    private String range;

    @JsonProperty("region_id")
    private Integer regionId;

    private String state;

    @JsonProperty("type_id")
    private Integer typeId;

    @JsonProperty("volume_remain")
    private Integer volumeRemain;

    @JsonProperty("volume_total")
    private Integer volumeTotal;
}
