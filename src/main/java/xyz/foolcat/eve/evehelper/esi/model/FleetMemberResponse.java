package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 舰队成员信息
 *
 * @author Leojan
 * date 2023-10-27 16:20
 */

@Data
@Tag(name = "舰队成员信息 200 ok")
public class FleetMemberResponse {

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("join_time")
    private OffsetDateTime joinTime;

    private String role;

    @JsonProperty("role_name")
    private String roleName;

    @JsonProperty("ship_type_id")
    private Integer shipTypeId;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("squad_id")
    private Long squadId;

    @JsonProperty("station_id")
    private Long stationId;

    @JsonProperty("takes_fleet_warp")
    private Boolean takesFleetWarp;

    @JsonProperty("wing_id")
    private Long wingId;
}
