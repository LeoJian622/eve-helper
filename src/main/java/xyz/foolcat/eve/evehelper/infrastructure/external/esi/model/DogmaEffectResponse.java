package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.Modifiers;

import java.util.List;

/**
 * 深渊属性影响详情
 *
 * @author Leojan
 * date 2023-10-26 15:58
 */

@Data
@Tag(name = "深渊属性影响详情 200 is ok")
public class DogmaEffectResponse {

    private String description;

    @JsonProperty("disallow_auto_repeat")
    private Boolean disallowAutoRepeat;

    @JsonProperty("discharge_attribute_id")
    private Integer dischargeAttributeId;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("duration_attribute_id")
    private Integer durationAttributeId;

    @JsonProperty("effect_category")
    private Integer effectCategory;

    @JsonProperty("effect_id")
    private Integer effectId;

    @JsonProperty("electronic_chance")
    private Boolean electronicChance;

    @JsonProperty("falloff_attribute_id")
    private Integer falloffAttributeId;

    @JsonProperty("icon_id")
    private Integer iconId;

    @JsonProperty("is_assistance")
    private Boolean isAssistance;

    @JsonProperty("is_offensive")
    private Boolean isOffensive;

    @JsonProperty("is_warp_safe")
    private Boolean isWarpSafe;

    private List<Modifiers> modifiers;

    private String name;

    @JsonProperty("post_expression")
    private Integer postExpression;

    @JsonProperty("pre_expression")
    private Integer preExpression;

    private Boolean published;

    @JsonProperty("range_attribute_id")
    private Integer rangeAttributeId;

    @JsonProperty("range_chance")
    private Boolean rangeChance;

    @JsonProperty("tracking_speed_attribute_id")
    private Integer trackingSpeedAttributeId;
}
