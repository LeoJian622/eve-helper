package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 工业设施清单
 *
 * @author Leojan
 * date 2023-10-25 16:11
 */

@Data
@Tag(name = "工业设施清单 200 ok")
public class FacilitiesResponse {

    @JsonProperty("facility_id")
    private Long facilityId;

    @JsonProperty("owner_id")
    private Integer ownerId;

    @JsonProperty("region_id")
    private Integer regionId;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    private Float tax;

    @JsonProperty("type_id")
    private Integer typeId;
}
