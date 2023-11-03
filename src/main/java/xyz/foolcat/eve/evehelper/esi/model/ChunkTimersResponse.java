package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 开采碎裂计时器
 *
 * @author Leojan
 * @date 2023-10-30 17:00
 */

@Data
@Tag(name = "开采碎裂计时器 200 ok")
public class ChunkTimersResponse {

    @JsonProperty("chunk_arrival_time")
    private OffsetDateTime chunkArrivalTime;

    @JsonProperty("extraction_start_time")
    private OffsetDateTime extractionStartTime;

    @JsonProperty("moon_id")
    private Integer moonId;

    @JsonProperty("natural_decay_time")
    private OffsetDateTime naturalDecayTime;

    @JsonProperty("structure_id")
    private Long structureId;

}
