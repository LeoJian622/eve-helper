package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 行星工厂生产详细信息
 *
 * @author Leojan
 * date 2023-11-07 15:47
 */

@Data
@Tag(name = "行星工厂生产详细信息  200 ok")
public class FactorySchematicResponse {

    @JsonProperty("cycle_time")
    private Integer cycleTime;

    @JsonProperty("schematic_name")
    private String schematicName;
}
