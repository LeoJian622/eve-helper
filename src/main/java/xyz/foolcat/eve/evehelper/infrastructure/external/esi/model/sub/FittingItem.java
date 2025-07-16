package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 舰船装配Item
 *
 * @author Leojan
 * date 2023-10-27 14:32
 */

@Data
@Tag(name = "属性影响")
public class FittingItem {

    private String flag;

    private Integer quantity;

    @JsonProperty("type_id")
    private Integer typeId;
}
