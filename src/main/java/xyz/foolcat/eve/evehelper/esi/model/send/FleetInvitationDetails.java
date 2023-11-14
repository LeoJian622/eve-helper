package xyz.foolcat.eve.evehelper.esi.model.send;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 舰队邀请
 *
 * @author Leojan
 * date 2023-10-27 16:54
 */

@Data
@Tag(name = "舰队邀请")
public class FleetInvitationDetails {

    @JsonProperty("character_id")
    private Integer characterId;

    /**
     * fleet_commander, wing_commander, squad_commander, squad_member
     */
    private String role;

    @JsonProperty("squad_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long squadId;

    @JsonProperty("wing_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long wingId;
}
