package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 *
 *  声望列表
 *
 * @author Leojan
 * date 2023-10-20 16:35
 */

@Data
@Tag(name = "声望列表响应体 200 is ok")
public class StandingResponse {

    @JsonProperty("from_id")
    private Integer fromId;

    @JsonProperty("from_type")
    private String fromType;

    private Float standing;
}
