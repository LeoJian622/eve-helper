package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Level;

import java.util.List;

/**
 * 舰船保险信息
 *
 * @author Leojan
 * @date 2023-10-31 11:43
 */

@Data
@Tag(name = "舰船保险信息 200 ok")
public class InsuranceOfShipResponse {

    private List<Level> levels;

    @JsonProperty("type_id")
    private Integer typeId;
}
