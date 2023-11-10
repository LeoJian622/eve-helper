package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 采集器详细信息
 *
 * @author Leojan
 * @date 2023-11-07 14:47
 */

@Data
@Tag(name = "采集器详细信息")
public class ExtractorDetail {

    @JsonProperty("cycle_time")
    private Integer cycleTime;

    @JsonProperty("head_radius")
    private Float headRadius;

    private List<Head>  heads;

    @JsonProperty("product_type_id")
    private Integer productTypeId;

    @JsonProperty("qty_per_cycle")
    private Integer qtyPerCycle;
}
