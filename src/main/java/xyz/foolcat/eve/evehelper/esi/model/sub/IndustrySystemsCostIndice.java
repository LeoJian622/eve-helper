package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 工业成本系数
 *
 * @author Leojan
 * @date 2023-10-31 10:59
 */

@Data
@Tag(name = "工业成本系数")
public class IndustrySystemsCostIndice {

    /**
     * copying, duplicating, invention, manufacturing, none, reaction, researching_material_efficiency, researching_technology, researching_time_efficiency, reverse_engineering
     */
    private String activity;

    @JsonProperty("cost_index")
    private Float costIndex;
}
