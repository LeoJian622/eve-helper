package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 军团公开信息
 *
 * @author Leojan
 * date 2023-10-25 14:38
 */

@Data
@Tag(name = "esi军团公开信息响应体 200 ok")
public class CorporationResponse {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("ceo_id")
    private Integer ceoId;

    @JsonProperty("creator_id")
    private Integer creatorId;

    @JsonProperty("date_founded")
    private OffsetDateTime dateFounded;

    private String description;

    @JsonProperty("faction_id")
    private Integer factionId;

    @JsonProperty("home_station_id")
    private Integer homeStationId;

    @JsonProperty("member_count")
    private Integer memberCount;

    private String name;

    private Long shares;

    @JsonProperty("tax_rate")
    private Float taxRate;

    private String ticker;

    private String title;

    @JsonProperty("war_eligible")
    private Boolean warEligible;
}
