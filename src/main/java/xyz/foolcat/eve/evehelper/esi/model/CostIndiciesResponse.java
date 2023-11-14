package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.IndustrySystemsCostIndice;

import java.util.List;

/**
 * 星系工业成本系数
 *
 * @author Leojan
 * date 2023-10-31 11:31
 */

@Data
@Tag(name = "星系工业成本系数 200 is ok")
public class CostIndiciesResponse {

    @JsonProperty("cost_indices")
    private List<IndustrySystemsCostIndice> costIndices;

    @JsonProperty("solar_system_id")
    private Integer solarSystemId;
}
