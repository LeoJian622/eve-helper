package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 基地空间站信息
 *
 * @author Leojan
 * @date 2023-10-24 11:24
 */

@Data
@Tag(name = "基地空间站信息")
public class HomeLocation {

    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("location_type")
    private String locationType;
}
