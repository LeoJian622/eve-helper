package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 燃料信息
 *
 * @author Leojan
 * date 2023-10-26 11:47
 */

@Data
@Tag(name = "燃料信息")
public class Fuels {

    private Integer quantity;

    @JsonProperty("type_id")
    private Integer typeId;
}
