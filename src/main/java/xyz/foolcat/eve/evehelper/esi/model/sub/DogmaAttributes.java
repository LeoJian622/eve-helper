package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 属性值
 *
 * @author Leojan
 * date 2023-10-26 15:33
 */

@Data
@Tag(name = "属性值")
public class DogmaAttributes {

    @JsonProperty("attribute_id")
    private Integer attributeId;

    private Float value;
}
