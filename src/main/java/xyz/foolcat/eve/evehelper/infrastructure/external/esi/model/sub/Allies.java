package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 盟友
 *
 * @author Leojan
 * date 2023-11-14 20:31
 */
@Data
@Tag(name = "盟友")
public class Allies {

    @JsonProperty("alliance_id")
    private Integer allianceId;

    @JsonProperty("corporation_id")
    private Integer corporationId;
}
