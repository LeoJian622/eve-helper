package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Position;

import java.util.List;

/**
 * 星座详情
 *
 * @author Leojan
 * date 2023-11-19 14:57
 */

@Data
@Tag(name = "星座详情 200 ok")
public class ConstellationInfoResponse {

    @JsonProperty("constellation_id")
    private Integer constellationId;

    private String name;

    private Position position;

    @JsonProperty("region_id")
    private Integer regionId;

    private List<Integer> systems;
}
