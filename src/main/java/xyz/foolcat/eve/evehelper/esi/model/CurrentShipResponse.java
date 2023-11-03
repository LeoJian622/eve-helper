package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 人物当前所在舰船信息
 *
 * @author Leojan
 * @date 2023-10-31 15:48
 */

@Data
@Tag(name = "人物当前所在舰船信息 200 ok")
public class CurrentShipResponse {

    @JsonProperty("ship_item_id")
    private  Long shipItemId;

    @JsonProperty("ship_name")
    private String shipName;

    @JsonProperty("ship_type_id")
    private Integer shipTypeId;
}
