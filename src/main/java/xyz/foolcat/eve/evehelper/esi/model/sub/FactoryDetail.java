package xyz.foolcat.eve.evehelper.esi.model.sub;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 工厂详细信
 *
 * @author Leojan
 * @date 2023-11-07 14:51
 */

@Data
@Tag(name = "工厂详细信")
public class FactoryDetail {

    @JsonProperty("schematic_id")
    private List<Integer> schematicId;
}
