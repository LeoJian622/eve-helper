package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.SovereigntyCampaignsParticipants;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 星系主权行动
 *
 * @author Leojan
 * date 2023-11-14 13:56
 */

@Data
@Tag(name = "星系主权行动 200 ok")
public class SovereigntyCampaignsResponse {


    @JsonProperty("attackers_score")
    private Float attackersScore;

    @JsonProperty("campaign_id")
    private Integer campaignId;

    @JsonProperty("constellation_id")
    private Integer constellationId;

    @JsonProperty("defender_id")
    private Integer defenderId;

    @JsonProperty("defender_score")
    private Float defenderScore;

    @JsonProperty("event_type")
    private String eventType;

    private List<SovereigntyCampaignsParticipants> participants;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("start_time")
    private OffsetDateTime startTime;

    @JsonProperty("structure_id")
    private Long structureId;
}
