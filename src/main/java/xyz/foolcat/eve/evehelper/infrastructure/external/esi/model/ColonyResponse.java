package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 行星殖民地信息
 *
 * @author Leojan
 * date 2023-11-07 13:59
 */

@Data
@Tag(name = "行星殖民地信息  200 ok")
public class ColonyResponse {

    @JsonProperty("last_update")
    private OffsetDateTime lastUpdate;

    @JsonProperty("num_pins")
    private Integer numPins;

    @JsonProperty("owner_id")
    private Integer ownerId;

    @JsonProperty("planet_id")
    private Integer planetId;

    @JsonProperty("planet_type")
    private String planetType;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("upgrade_level")
    private Integer upgradeLevel;
}
