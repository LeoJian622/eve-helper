package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 恒星详情
 *
 * @author Leojan
 * date 2023-11-19 17:06
 */

@Data
@Tag(name = "恒星详情 200 ok")
public class StarInfoResponse {

    private Long age;

    private Float luminosity;

    private String name;

    private Long radius;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;

    @JsonProperty("spectral_class")
    private String spectralClass;

    private Integer temperature;

    @JsonProperty("type_id")
    private Integer typeId;
}
