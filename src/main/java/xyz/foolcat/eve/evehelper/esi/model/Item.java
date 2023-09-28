package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * character_id_bookmarks_item
 *
 * @author Leojan
 * @date 2023-09-28 11:34
 */

@Data
public class Item {

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("type_id")
    private Integer typeId;
}
