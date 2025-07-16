package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 联盟参与的分数
 *
 * @author Leojan
 * date 2023-11-14 13:54
 */

@Data
@Tag(name = "联盟参与的分数")
public class SovereigntyCampaignsParticipants {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    private Float score;

}
