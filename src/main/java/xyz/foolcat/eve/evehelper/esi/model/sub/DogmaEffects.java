package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 属性影响
 *
 * @author Leojan
 * date 2023-10-26 15:34
 */

@Data
@Tag(name = "属性影响")
public class DogmaEffects {

    @JsonProperty("effect_id")
    private Integer effectId;

    @JsonProperty("is_default")
    private Boolean isDefault;
}
