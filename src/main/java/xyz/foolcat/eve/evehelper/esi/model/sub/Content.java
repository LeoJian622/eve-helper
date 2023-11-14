package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 节点内容信息
 *
 * @author Leojan
 * date 2023-11-07 14:39
 */

@Data
@Tag(name = "节点内容信息")
public class Content {

    private Long amount;

    @JsonProperty("type_id")
    private Integer typeId;
}
