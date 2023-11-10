package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 节点信息
 *
 * @author Leojan
 * @date 2023-11-07 14:37
 */

@Data
@Tag(name = "节点信息")
public class Pin {

    private List<Content> contents;

    @JsonProperty("expiry_time")
    private OffsetDateTime expiryTime;

    @JsonProperty("extractor_details")
    private ExtractorDetail extractorDetails;

    @JsonProperty("factory_details")
    private List<FactoryDetail> factoryDetails;

    @JsonProperty("install_time")
    private OffsetDateTime installTime;

    @JsonProperty("last_cycle_start")
    private OffsetDateTime lastCycleStart;

    private Float latitude;

    private Float longitude;

    @JsonProperty("pin_id")
    private Long pinId;

    @JsonProperty("schematic_id")
    private Integer schematicId;

    @JsonProperty("type_id")
    private Integer typeId;
}
