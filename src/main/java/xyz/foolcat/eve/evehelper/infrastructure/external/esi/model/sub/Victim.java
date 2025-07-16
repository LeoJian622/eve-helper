package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 击毁报告 受害者
 *
 * @author Leojan
 * date 2023-10-31 14:16
 */

@Data
@Tag(name = "击毁报告 受害者")
public class Victim {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("corporation_id")
    private Integer corporationId;

    @JsonProperty("damage_taken")
    private Integer damageTaken;

    @JsonProperty("faction_id")
    private Integer factionId;

    private List<KillItem> items;

    private Position position;

    @JsonProperty("ship_type_id")
    private Integer shipTypeId;

}
