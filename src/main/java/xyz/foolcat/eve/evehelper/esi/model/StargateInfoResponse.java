package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Destination;
import xyz.foolcat.eve.evehelper.esi.model.sub.Position;

/**
 * 星门信息
 *
 * @author Leojan
 * date 2023-11-19 16:57
 */

@Data
@Tag(name = "星门信息 200 ok")
public class StargateInfoResponse {

    private Destination destination;

    private String name;

    private Position position;

    @JsonProperty("stargate_id")
    private Integer stargateId;

    @JsonProperty("system_id")
    private Integer systemId;

    @JsonProperty("type_id")
    private Integer typeId;
}
