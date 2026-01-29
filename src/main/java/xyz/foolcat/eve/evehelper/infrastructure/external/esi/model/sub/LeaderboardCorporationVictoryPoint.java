package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 军团势力战争积分
 *
 * @author Leojan
 * date 2023-10-26 16:19
 */

@Data
@Tag(name = "军团势力战争积分")
public class LeaderboardCorporationVictoryPoint {

    @JsonProperty("active_total")
    private List<LeaderboardCorporationInfo> activeTotal;

    @JsonProperty("last_week")
    private List<LeaderboardCorporationInfo> lastWeek;

    private List<LeaderboardCorporationInfo> yesterday;
}
