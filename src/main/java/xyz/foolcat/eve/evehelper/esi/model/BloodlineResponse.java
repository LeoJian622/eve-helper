package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 游戏内种族信息
 *
 * @author Leojan
 * date 2023-11-19 11:12
 */

@Data
@Tag(name = "游戏内种族信息 200 ok")
public class BloodlineResponse {

    @JsonProperty("bloodline_id")
    private Integer bloodlineId;

    private Integer charisma;

    @JsonProperty("corporation_id")
    private Integer corporationId;

    private String description;

    private Integer intelligence;

    private Integer memory;

    private String name;

    private Integer perception;

    @JsonProperty("race_id")
    private Integer raceId;

    @JsonProperty("ship_type_id")
    private Integer shipTypeId;

    private Integer willpower;
}
