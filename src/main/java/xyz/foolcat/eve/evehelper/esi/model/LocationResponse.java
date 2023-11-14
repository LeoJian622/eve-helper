package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 人物当前位置的相关信息
 *
 * @author Leojan
 * date 2023-10-31 15:15
 */

@Data
@Tag(name = "人物当前位置的相关信息 200 ok")
public class LocationResponse {

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("station_id")
    private Integer stationId;

    @JsonProperty("structure_id")
    private Long structureId;
}
