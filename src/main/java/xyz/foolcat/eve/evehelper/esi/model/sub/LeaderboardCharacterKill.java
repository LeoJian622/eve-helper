package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 势力战争击杀
 *
 * @author Leojan
 * date 2023-10-26 16:58
 */

@Data
@Tag(name = "人物势力战争击杀")
public class LeaderboardCharacterKill {

    @JsonProperty("active_total")
    private List<LeaderboardCharacterInfo> activeTotal;

    @JsonProperty("last_week")
    private List<LeaderboardCharacterInfo> lastWeek;

    private List<LeaderboardCharacterInfo> yesterday;
}
