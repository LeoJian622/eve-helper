package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Fuels;

import java.util.List;

/**
 * 军团母星建筑（POS）配置信息
 *
 * @author Leojan
 * date 2023-10-26 11:45
 */

@Data
@Tag(name = "军团母星建筑（POS）配置信息 200 is ok")
public class StarBaseConfigResponse {

    @JsonProperty("allow_alliance_members")
    private Boolean allowAllianceMembers;

    @JsonProperty("allow_corporation_members")
    private Boolean allowCorporationMembers;

    private String anchor;

    @JsonProperty("attack_if_at_war")
    private Boolean attackIfAtWar;

    @JsonProperty("attack_if_other_security_status_dropping")
    private Boolean attackIfOtherSecurityStatusDropping;

    @JsonProperty("attack_security_status_threshold")
    private Float attackSecurityStatusThreshold;

    @JsonProperty("attack_standing_threshold")
    private Float attackStandingThreshold;

    @JsonProperty("fuel_bay_take")
    private String fuelBayTake;

    @JsonProperty("fuel_bay_view")
    private String fuelBayView;

    private List<Fuels> fuels;

    private String offline;

    private String online;

    private String unanchor;

    @JsonProperty("use_alliance_standings")
    private Boolean useAllianceStandings;
}
