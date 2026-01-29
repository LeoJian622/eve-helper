package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 主权地图信息
 *
 * @author Leojan
 * date 2023-11-14 19:52
 */

@Data
@Tag(name = "主权地图信息 200 ok")
public class SovereigntyMapResponse {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("corporation_id")
    private Integer corporationId;

    @JsonProperty("faction_id")
    private Integer factionId;

    @JsonProperty("system_id")
    private Integer systemId;
}
