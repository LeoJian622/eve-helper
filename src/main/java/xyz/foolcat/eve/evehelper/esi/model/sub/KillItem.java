package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 击毁报告item
 *
 * @author Leojan
 * date 2023-10-31 14:12
 */

@Data
@Tag(name = "击毁报告item")
public class KillItem {

    private Integer flag;

    @JsonProperty("item_type_id")
    private Integer itemTypeId;

    @JsonProperty("quantity_destroyed")
    private Long quantityDestroyed;

    @JsonProperty("quantity_dropped")
    private Long quantityDropped;

    private Integer singleton;
}
