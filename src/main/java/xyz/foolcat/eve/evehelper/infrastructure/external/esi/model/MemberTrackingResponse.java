package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 成员追踪信息
 *
 * @author Leojan
 * date 2023-10-26 10:08
 */

@Data
@Tag(name = "成员追踪信息响应体 200 ok")
public class MemberTrackingResponse {

    @JsonProperty("base_id")
    private Integer baseId;

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("logoff_date")
    private OffsetDateTime logoffDate;

    @JsonProperty("logon_date")
    private OffsetDateTime logonDate;

    @JsonProperty("ship_type_id")
    private Integer shipTypeId;

    @JsonProperty("start_date")
    private OffsetDateTime startDate;
}
