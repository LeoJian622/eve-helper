package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 战争势力信息
 *
 * @author Leojan
 * date 2023-11-14 20:25
 */

@Data
@Tag(name = "战争势力信息")
public class Combatants {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("corporation_id")
    private Integer corporationId;

    @JsonProperty("isk_destroyed")
    private Float iskDestroyed;

    @JsonProperty("ships_killed")
    private Integer shipsKilled;
}
