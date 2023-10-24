package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Position;

/**
 * @author Leojan
 * @date 2023-09-27 17:28
 */

@Data
@Tag(name = "资产位置响应体 200 ok")
public class AssetsLocationResponse {

    @JsonProperty( "item_id")
    private Long itemId;

    private Position position;
}
