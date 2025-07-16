package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 星域价格历史统计
 *
 * @author Leojan
 * date 2023-11-03 15:42
 */

@Data
@Tag(name = "星域价格历史统计 200 ok")
public class HistoricalMarketStatisticsResponse {

    private Double average;

    private OffsetDateTime date;
    
    private Double highest;

    private Double lowest;

    @JsonProperty("order_count")
    private Long orderCount;

    private Long volume;
}
