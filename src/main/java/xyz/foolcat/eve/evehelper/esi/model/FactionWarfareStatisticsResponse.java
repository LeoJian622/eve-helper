package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Kill;
import xyz.foolcat.eve.evehelper.esi.model.sub.VictoryPoint;

import java.time.OffsetDateTime;

/**
 * 势力战争统计
 *
 * @author Leojan
 * @date 2023-10-26 16:32
 */

@Data
@Tag(name = "势力战争统计 200 ok")
public class FactionWarfareStatisticsResponse {

    @JsonProperty("current_rank")
    private Integer currentRank;

    @JsonProperty("enlisted_on")
    private OffsetDateTime enlistedOn;

    @JsonProperty("faction_id")
    private Integer factionId;

    @JsonProperty("highest_rank")
    private Integer highestRank;

    private Kill kills;

    private Integer pilots;

    @JsonProperty("systems_controlled")
    private Integer systemsControlled;

    @JsonProperty("victory_points")
    private VictoryPoint victoryPoints;
}
