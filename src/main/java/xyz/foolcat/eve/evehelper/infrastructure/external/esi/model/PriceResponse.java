package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 物品价格信息
 *
 * @author Leojan
 * date 2023-11-03 16:50
 */

@Data
@Tag(name = "物品价格信息 200 ok")
public class PriceResponse {

    @JsonProperty("adjusted_price")
    private Double adjustedPrice;

    @JsonProperty("average_price")
    private Double averagePrice;

    @JsonProperty("type_id")
    private Integer typeId;
}
