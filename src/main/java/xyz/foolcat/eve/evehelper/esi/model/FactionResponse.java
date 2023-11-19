package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 势力
 *
 * @author Leojan
 * date 2023-11-19 15:08
 */

@Data
@Tag(name = "势力 200 ok")
public class FactionResponse {

    @JsonProperty("corporation_id")
    private Integer corporationId;

    private String description;

    @JsonProperty("faction_id")
    private Integer factionId;

    @JsonProperty("is_unique")
    private Boolean isUnique;

    @JsonProperty("militia_corporation_id")
    private Integer militiaCorporationId;

    private String name;

    @JsonProperty("size_factor")
    private Float sizeFactor;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("station_count")
    private Integer stationCount;

    @JsonProperty("station_system_count")
    private Integer stationSystemCount;
}
