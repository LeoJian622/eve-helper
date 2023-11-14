package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 击毁报告 攻击者
 *
 * @author Leojan
 * date 2023-10-31 14:23
 */

@Data
@Tag(name = "击毁报告 攻击者")
public class Attacker {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("corporation_id")
    private Integer corporationId;

    @JsonProperty("damage_done")
    private Integer damageTaken;

    @JsonProperty("faction_id")
    private Integer factionId;

    @JsonProperty("final_blow")
    private Boolean finalBlow;

    @JsonProperty("security_status")
    private Float securityStatus;

    @JsonProperty("ship_type_id")
    private Integer shipTypeId;

    @JsonProperty("weapon_type_id")
    private Integer weaponTypeId;
}
