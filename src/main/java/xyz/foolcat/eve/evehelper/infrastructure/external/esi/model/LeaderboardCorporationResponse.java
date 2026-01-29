package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.LeaderboardCorporationKill;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.LeaderboardCorporationVictoryPoint;

/**
 * 军团势力战争排行榜
 *
 * @author Leojan
 * date 2023-10-26 16:46
 */

@Data
@Tag(name = "军团势力战争排行榜 200 ok")
public class LeaderboardCorporationResponse {

    private LeaderboardCorporationKill kills;

    @JsonProperty("victory_points")
    private LeaderboardCorporationVictoryPoint victoryPoints;
}
