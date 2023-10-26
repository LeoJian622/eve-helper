package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.StructuresService;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 军团建筑信息
 *
 * @author Leojan
 * @date 2023-10-26 13:56
 */

@Data
@Tag(name = "军团建筑信息 200 is ok")
public class StructuresInformationResponse {

    @JsonProperty("corporation_id")
    private Integer corporationId;

    @JsonProperty("fuel_expires")
    private OffsetDateTime fuelExpires;

    private String name;

    @JsonProperty("next_reinforce_apply")
    private OffsetDateTime nextReinforceApply;

    @JsonProperty("next_reinforce_hour")
    private Integer nextReinforceHour;

    @JsonProperty("profile_id")
    private Integer profileId;

    @JsonProperty("reinforce_hour")
    private Integer reinforceHour;

    private List<StructuresService> services;

    private String state;

    @JsonProperty("state_timer_end")
    private OffsetDateTime stateTimerEnd;

    @JsonProperty("state_timer_start")
    private OffsetDateTime stateTimerStart;

    @JsonProperty("structure_id")
    private Long structureId;

    @JsonProperty("system_id")
    private Integer systemId;

    @JsonProperty("type_id")
    private Integer typeId;

    @JsonProperty("unanchors_at")
    private OffsetDateTime unanchorsAt;
}
