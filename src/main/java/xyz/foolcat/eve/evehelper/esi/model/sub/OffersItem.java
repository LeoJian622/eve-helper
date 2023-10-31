package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 忠诚点商店需求item
 *
 * @author Leojan
 * @date 2023-10-31 16:11
 */
@Data
@Tag(name = "忠诚点商店需求item")
public class OffersItem {

    private Integer quantity;

    @JsonProperty("type_id")
    private Integer typeId;
}
