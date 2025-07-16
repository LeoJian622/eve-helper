package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 恒星系天体列表
 *
 * @author Leojan
 * date 2023-11-19 19:23
 */

@Data
@Tag(name = "恒星系天体列表")
public class Planet {

    @JsonProperty("asteroid_belts")
    private List<Integer> asteroidBelts;

    private List<Integer> moons;

    @JsonProperty("planet_id")
    private Integer planetId;
}
