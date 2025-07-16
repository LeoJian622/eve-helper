package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Position;

/**
 * 建筑详情
 *
 * @author Leojan
 * date 2023-11-19 17:23
 */

@Data
@Tag(name = "建筑详情 200 ok")
public class StructureInfoResponse {

    private String name;

    @JsonProperty("owner_id")
    private Integer ownerId;

    private Position position;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("type_id")
    private Integer typeId;
}
