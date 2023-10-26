package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 军团母星建筑（POS）
 *
 * @author Leojan
 * @date 2023-10-26 11:36
 */

@Data
@Tag(name = "军团母星建筑（POS） 200 is ok")
public class StarBaseResponse {

    @JsonProperty("moon_id")
    private Integer moonId;

    @JsonProperty("onlined_since")
    private OffsetDateTime onlinedSince;

    @JsonProperty("reinforced_until")
    private OffsetDateTime reinforcedUntil;

    @JsonProperty("starbase_id")
    private Long starbaseId;

    private String state;

    @JsonProperty("system_id")
    private Integer systemId;

    @JsonProperty("type_id")
    private Integer typeId;

    @JsonProperty("unanchor_at")
    private OffsetDateTime unanchorAt;
}
