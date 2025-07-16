package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 属性详情
 *
 * @author Leojan
 * date 2023-10-26 15:19
 */

@Data
@Tag(name = "属性详情信息 200 is ok")
public class DogmaAttributeResponse {

    @JsonProperty("attribute_id")
    private Integer attributeId;

    @JsonProperty("default_value")
    private Float defaultValue;

    private String description;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("high_is_good")
    private Boolean highIsGood;

    @JsonProperty("icon_id")
    private Integer iconId;

    private String name;

    private Boolean published;

    private Boolean stackable;

    @JsonProperty("unit_id")
    private Integer unitId;
}
