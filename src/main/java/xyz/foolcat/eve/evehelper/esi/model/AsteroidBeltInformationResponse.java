package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Position;

/**
 * 小行星带信息
 *
 * @author Leojan
 * date 2023-11-18 23:02
 */

@Data
@Tag(name = "小行星带信息 200 ok")
public class AsteroidBeltInformationResponse {

    private String name;

    private Position position;

    @JsonProperty("system_id")
    private Integer systemId;
}
