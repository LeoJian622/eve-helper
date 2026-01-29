package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 *
 * 链接信息
 *
 * @author Leojan
 * date 2023-11-07 14:34
 */

@Data
@Tag(name = "链接信息")
public class Link {

    @JsonProperty("destination_pin_id")
    private Long destinationPinId;

    @JsonProperty("link_level")
    private Integer linkLevel;

    @JsonProperty("source_pin_id")
    private Long sourcePinId;
}
