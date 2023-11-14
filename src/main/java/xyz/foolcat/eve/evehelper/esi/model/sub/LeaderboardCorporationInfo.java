package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 军团排行榜排行信息
 *
 * @author Leojan
 * date 2023-10-26 16:50
 */

@Data
@Tag(name = "军团排行榜排行信息")
public class LeaderboardCorporationInfo {

    private Integer amount;

    @JsonProperty("corporation_id")
    private Integer corporationId;
}
