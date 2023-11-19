package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Position;

/**
 * 行星详情
 *
 * @author Leojan
 * date 2023-11-19 16:25
 */

@Data
@Tag(name = "行星详情 200 ok")
public class PlanetInfoResponse {

    private String name;

    @JsonProperty("planet_id")
    private Integer planetId;

    private Position position;

    @JsonProperty("system_id")
    private Integer systemId;

    @JsonProperty("type_id")
    private Integer typeId;

}
