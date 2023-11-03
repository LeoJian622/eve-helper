package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 忠诚点信息
 *
 * @author Leojan
 * @date 2023-10-31 16:01
 */

@Data
@Tag(name = "忠诚点信息 200 ok")
public class LoyaltyPointsResponse {

    @JsonProperty("corporation_id")
    private Integer corporationId;

    @JsonProperty("loyalty_points")
    private Integer loyaltyPoints;
}
