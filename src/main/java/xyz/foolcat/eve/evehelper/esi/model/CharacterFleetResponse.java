package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 人物的舰队基础信息
 *
 * @author Leojan
 * date 2023-10-27 15:25
 */

@Data
@Tag(name = "人物的舰队基础信息 200 ok")
public class CharacterFleetResponse {

    @JsonProperty("fleet_id")
    private Long fleetId;

    private String role;

    @JsonProperty("squad_id")
    private Long squadId;

    @JsonProperty("wing_id")
    private Long wingId;
}
