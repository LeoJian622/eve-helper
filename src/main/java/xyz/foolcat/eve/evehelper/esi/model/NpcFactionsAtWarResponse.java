package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 战争中的NPC势力
 *
 * @author Leojan
 * date 2023-10-27 14:11
 */

@Data
@Tag(name = "战争中的NPC势力 200 ok")
public class NpcFactionsAtWarResponse {

    @JsonProperty("against_id")
    private Integer againstId;

    @JsonProperty("faction_id")
    private Integer factionId;
}
