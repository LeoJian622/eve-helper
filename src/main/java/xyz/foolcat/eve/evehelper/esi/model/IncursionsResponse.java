package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 入侵星系信息
 *
 * @author Leojan
 * date 2023-10-30 15:33
 */

@Data
@Tag(name = "入侵星系信息 200 ok")
public class IncursionsResponse {

    @JsonProperty("constellation_id")
    private Integer constellationId;

    @JsonProperty("faction_id")
    private Integer factionId;

    @JsonProperty("has_boss")
    private Boolean hasBoss;

    @JsonProperty("infested_solar_systems")
    private List<Integer> infestedSolarSystems;

    private Float influence;

    @JsonProperty("staging_solar_system_id")
    private Integer stagingSolarSystemId;

    private String state;

    private String type;
}
