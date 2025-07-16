package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 势力战争星系所有权信息
 *
 * @author Leojan
 * date 2023-10-27 11:28
 */

@Data
@Tag(name = "势力战争星系所有权信息 200 ok")
public class FactionWarfareSolarSystemsResponse {

    private String contested;

    @JsonProperty("occupier_faction_id")
    private Integer occupierFactionId;

    @JsonProperty("owner_faction_id")
    private Integer ownerFactionId;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("victory_points")
    private Integer victoryPoints;

    @JsonProperty("victory_points_threshold")
    private Integer victoryPointsThreshold;
}
