package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 主权建筑信息
 *
 * @author Leojan
 * date 2023-11-14 19:56
 */

@Data
@Tag(name = "主权地图信息 200 ok")
public class SovereigntyStructuresResponse {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("structure_id")
    private Long structureId;

    @JsonProperty("structure_type_id")
    private Integer structureTypeId;

    @JsonProperty("vulnerability_occupancy_level")
    private Float vulnerabilityOccupancyLevel;

    @JsonProperty("vulnerable_end_time")
    private OffsetDateTime vulnerableEndTime;

    @JsonProperty("vulnerable_start_time")
    private OffsetDateTime vulnerableStartTime;

}
