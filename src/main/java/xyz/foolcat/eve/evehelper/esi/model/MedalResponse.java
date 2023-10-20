package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 勋章信息
 *
 * @author Leojan
 * @date 2023-10-20 14:45
 */
@Data
@Tag(name = "勋章信息响应体 200 ok")
public class MedalResponse {

    @JsonProperty("corporation_id")
    private Integer corporationId;

    private OffsetDateTime date;

    private String description;

    private Graphics[] graphics;

    @JsonProperty("issuer_id")
    private Integer issuerId;

    @JsonProperty("medal_id")
    private Integer medalId;

    private String reason;

    private String status;

    private String title;
}
