package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 星域信息
 *
 * @author Leojan
 * date 2023-11-19 16:48
 */

@Data
@Tag(name = "星域信息 200 ok")
public class RegionInfoResponse {

    private List<Integer> constellations;

    private String description;

    private String name;

    @JsonProperty("region_id")
    private Integer regionId;
}
