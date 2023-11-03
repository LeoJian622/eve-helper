package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.LeaderboardKill;
import xyz.foolcat.eve.evehelper.esi.model.sub.LeaderboardVictoryPoint;

/**
 * 势力战争排行榜
 *
 * @author Leojan
 * @date 2023-10-26 16:46
 */

@Data
@Tag(name = "势力战争排行榜 200 ok")
public class LeaderboardResponse {

    private LeaderboardKill kills;

    @JsonProperty("victory_points")
    private LeaderboardVictoryPoint victoryPoints;
}
