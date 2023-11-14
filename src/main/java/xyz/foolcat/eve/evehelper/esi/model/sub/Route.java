package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 线路信息
 *
 * @author Leojan
 * date 2023-11-07 15:15
 */

@Data
@Tag(name = "线路信息")
public class Route {

    @JsonProperty("content_type_id")
    private Integer contentTypeId;

    @JsonProperty("destination_pin_id")
    private Long destinationPinId;

    private Float quantity;

    @JsonProperty("route_id")
    private Long routeId;

    @JsonProperty("source_pin_id")
    private Long sourcePinId;

    private List<Long> waypoints;
}
