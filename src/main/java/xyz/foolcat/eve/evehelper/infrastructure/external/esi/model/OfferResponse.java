package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.OffersItem;

import java.util.List;

/**
 * 忠诚点商店兑换列表
 *
 * @author Leojan
 * date 2023-10-31 16:13
 */

@Data
@Tag(name = "忠诚点商店兑换列表 200 ok")
public class OfferResponse {

    @JsonProperty("ak_cost")
    private Integer akCost;

    @JsonProperty("isk_cost")
    private Long iskCost;

    @JsonProperty("lp_cost")
    private Integer lpCost;

    @JsonProperty("offer_id")
    private Integer offerId;

    private Integer quantity;

    @JsonProperty("required_items")
    private List<OffersItem> requiredItems;

    @JsonProperty("type_id")
    private Integer typeId;
}
