package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 修正信息
 *
 * @author Leojan
 * date 2023-10-26 15:59
 */

@Data
@Tag(name = "修正信息")
public class Modifiers {

    private String domain;

    @JsonProperty("effect_id")
    private Integer effectId;

    private String func;

    @JsonProperty("modified_attribute_id")
    private Integer modifiedAttributeId;

    @JsonProperty("modifying_attribute_id")
    private Integer modifyingAttributeId;

    private Integer operator;
}
