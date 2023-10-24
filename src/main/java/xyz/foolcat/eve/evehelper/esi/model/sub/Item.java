package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * character_id_bookmarks_item
 *
 * @author Leojan
 * @date 2023-09-28 11:34
 */

@Data
@Tag(name = "物品信息")
public class Item {

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("type_id")
    private Integer typeId;
}
