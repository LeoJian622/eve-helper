package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @author Leojan
 * date 2023-10-18 17:10
 */

@Data
@Tag(name = "人物公开信息响应体 200 ok")
public class CharacterPublicInfoResponse {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    private OffsetDateTime birthday;

    @JsonProperty("bloodline_id")
    private Integer bloodlineId;

    @JsonProperty("corporation_id")
    private Integer corporationId;

    private String description;

    @JsonProperty("faction_id")
    private Integer factionId;

    private String gender;

    private String name;

    @JsonProperty("race_id")
    private Integer raceId;

    @JsonProperty("security_status")
    private Double securityStatus;

    private String title;
}
