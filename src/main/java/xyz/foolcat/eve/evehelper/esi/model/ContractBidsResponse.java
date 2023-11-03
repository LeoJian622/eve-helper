package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 合同（拍卖）出价信息
 *
 * @author Leojan
 * @date 2023-10-24 17:05
 */

@Data
@Tag(name = "esi合同（拍卖）出价信息响应体 200 ok")
public class ContractBidsResponse {

    private Float amount;

    @JsonProperty("bid_id")
    private Integer bidId;

    @JsonProperty("bidder_id")
    private Integer bidderId;

    @JsonProperty("date_bid")
    private OffsetDateTime dateBid;
}
