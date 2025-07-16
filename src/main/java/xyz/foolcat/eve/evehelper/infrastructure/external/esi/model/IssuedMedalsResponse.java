package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 军团发布的勋章信息
 *
 * @author Leojan
 * date 2023-10-25 16:51
 */

@Data
@Tag(name = "军团发布的勋章信息响应体 200 ok")
public class IssuedMedalsResponse {

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("issued_at")
    private OffsetDateTime issuedAt;

    @JsonProperty("issuer_id")
    private Integer issuerId;

    @JsonProperty("medal_id")
    private Integer medalId;

    private String reason;

    private String status;
}
