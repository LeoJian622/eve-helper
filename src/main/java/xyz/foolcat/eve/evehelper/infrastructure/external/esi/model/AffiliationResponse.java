package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 人物的军团、联盟、势力ID
 *
 * @author Leojan
 * date 2023-10-20 16:59
 */

@Data
@Tag(name = "人物的军团、联盟、势力ID响应体 200 ok")
public class AffiliationResponse {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("corporation_id")
    private Integer corporationId;

    @JsonProperty("faction_id")
    private Integer factionId;
}
