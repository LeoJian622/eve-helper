package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 星系击杀详情
 *
 * @author Leojan
 * date 2023-11-19 17:34
 */

@Data
@Tag(name = "星系击杀详情 200 ok")
public class SolarSystemKillResponse {

    @JsonProperty("npc_kills")
    private Integer npcKills;

    @JsonProperty("pod_kills")
    private Integer podKills;

    @JsonProperty("ship_kills")
    private Integer shipKills;

    @JsonProperty("system_id")
    private Integer systemId;
}
