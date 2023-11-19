package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Planet;
import xyz.foolcat.eve.evehelper.esi.model.sub.Position;

import java.util.List;

/**
 * 恒星系详情
 *
 * @author Leojan
 * date 2023-11-19 19:28
 */

@Data
@Tag(name = "恒星系详情 200 ok")
public class SolarSystemInfoResponse {


    @JsonProperty("constellation_id")
    private Integer constellationId;

    private String name;

    private List<Planet> planets;

    private Position position;

    @JsonProperty("security_class")
    private String securityClass;

    @JsonProperty("security_status")
    private Float securityStatus;

    @JsonProperty("star_id")
    private Integer starId;

    private List<Integer> stargates;

    private List<Integer> stations;

    @JsonProperty("system_id")
    private Integer systemId;
}
