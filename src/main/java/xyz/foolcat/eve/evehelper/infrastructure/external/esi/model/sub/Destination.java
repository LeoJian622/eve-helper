package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 目的地
 *
 * @author Leojan
 * date 2023-11-19 16:56
 */

@Data
@Tag(name = "目的地")
public class Destination {

    @JsonProperty("stargate_id")
    private Integer stargateId;

    @JsonProperty("system_id")
    private Integer systemId;
}
