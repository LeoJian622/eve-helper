package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 势力战争击杀
 *
 * @author Leojan
 * @date 2023-10-26 16:58
 */

@Data
@Tag(name = "军团势力战争击杀")
public class LeaderboardCorporationKill {

    @JsonProperty("active_total")
    private List<LeaderboardCorporationInfo> activeTotal;

    @JsonProperty("last_week")
    private List<LeaderboardCorporationInfo> lastWeek;

    private List<LeaderboardCorporationInfo> yesterday;
}
