package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 军团设施信息
 *
 * @author Leojan
 * date 2023-10-25 16:11
 */

@Data
@Tag(name = "军团设施信息响应体 200 ok")
public class CorporationFacilitiesResponse {

    @JsonProperty("facility_id")
    private Long facilityId;

    @JsonProperty("system_id")
    private Integer systemId;

    @JsonProperty("type_id")
    private Integer typeId;
}
