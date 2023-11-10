package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 军团的海关信息及配置
 *
 * @author Leojan
 * @date 2023-11-07 15:35
 */

@Data
@Tag(name = "军团的海关信息及配置  200 ok")
public class CustomsOfficesSettingResponse {

    @JsonProperty("alliance_tax_rate")
    private Float allianceTaxRate;

    @JsonProperty("allow_access_with_standings")
    private Boolean allowAccessWithStandings;

    @JsonProperty("allow_alliance_access")
    private Boolean allowAllianceAccess;

    @JsonProperty("bad_standing_tax_rate")
    private Float badStandingTaxRate;

    @JsonProperty("corporation_tax_rate")
    private Float corporationTaxRate;

    @JsonProperty("excellent_standing_tax_rate")
    private Float excellentStandingTaxRate;

    @JsonProperty("good_standing_tax_rate")
    private Float goodStandingTaxRate;

    @JsonProperty("neutral_standing_tax_rate")
    private Float neutralStandingTaxRate;

    @JsonProperty("office_id")
    private Long officeId;

    @JsonProperty("reinforce_exit_end")
    private Integer reinforceExitEnd;

    @JsonProperty("reinforce_exit_start")
    private Integer reinforceExitStart;

    @JsonProperty("standing_level")
    private String standingLevel;

    @JsonProperty("system_id")
    private Integer systemId;

    @JsonProperty("terrible_standing_tax_rate")
    private Float terribleStandingTaxRate;
}
