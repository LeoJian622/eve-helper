package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.DogmaAttributes;
import xyz.foolcat.eve.evehelper.esi.model.sub.DogmaEffects;

import java.util.List;

/**
 * 深渊装备属性
 *
 * @author Leojan
 * date 2023-10-26 15:42
 */

@Data
@Tag(name = "深渊装备属性 200 is ok")
public class DynamicItemResponse {

    @JsonProperty("created_by")
    private Integer createdBy;

    @JsonProperty("dogma_attributes")
    private List<DogmaAttributes> dogmaAttributes;

    @JsonProperty("dogma_effects")
    private List<DogmaEffects> dogmaEffects;

    @JsonProperty("mutator_type_id")
    private Integer mutatorTypeId;

    @JsonProperty("source_type_id")
    private Integer sourceTypeId;
}
