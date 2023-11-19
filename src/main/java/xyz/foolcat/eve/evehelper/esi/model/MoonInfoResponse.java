package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Position;

/**
 * 卫星信息
 *
 * @author Leojan
 * date 2023-11-19 16:12
 */

@Data
@Tag(name = "卫星信息 200 ok")
public class MoonInfoResponse {

    @JsonProperty("moon_id")
    private Integer moonId;

    private String name;

    private Position position;

    @JsonProperty("system_id")
    private Integer systemId;
}
