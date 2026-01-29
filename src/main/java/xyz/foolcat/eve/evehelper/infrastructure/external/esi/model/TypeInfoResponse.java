package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.DogmaAttributes;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.DogmaEffects;

import java.util.List;

/**
 * 类型ID详情
 *
 * @author Leojan
 * date 2023-11-19 19:49
 */

@Data
@Tag(name = "类型ID详情 200 ok")
public class TypeInfoResponse {

    private Float capacity;

    private String description;

    @JsonProperty("dogma_attributes")
    private List<DogmaAttributes> dogmaAttributes;

    @JsonProperty("dogma_effects")
    private List<DogmaEffects> dogmaEffects;

    @JsonProperty("graphic_id")
    private Integer graphicId;

    @JsonProperty("group_id")
    private Integer groupId;

    @JsonProperty("icon_id")
    private Integer iconId;

    @JsonProperty("market_group_id")
    private Integer marketGroupId;

    private Float mass;

    private String name;

    @JsonProperty("packaged_volume")
    private Float packagedVolume;

    @JsonProperty("portion_size")
    private Integer portionSize;

    private Boolean published;

    private Float radius;

    @JsonProperty("type_id")
    private Integer typeId;

    private Float volume;
}
