package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 星系跳跃次数
 *
 * @author Leojan
 * date 2023-11-19 17:30
 */

@Data
@Tag(name = "星系跳跃次数 200 ok")
public class SolarSystemJumpResponse {

    @JsonProperty("ship_jumps")
    private Integer shipJumps;

    @JsonProperty("system_id")
    private Integer systemId;
}
