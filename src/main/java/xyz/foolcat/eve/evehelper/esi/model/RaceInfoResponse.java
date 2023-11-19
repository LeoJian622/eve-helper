package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 人物种族信息
 *
 * @author Leojan
 * date 2023-11-19 16:37
 */

@Data
@Tag(name = "人物种族信息 200 ok")
public class RaceInfoResponse {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    private String description;

    private String name;

    @JsonProperty("race_id")
    private Integer raceId;
}
