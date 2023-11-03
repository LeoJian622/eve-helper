package xyz.foolcat.eve.evehelper.esi.model.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.FittingItem;

import java.util.List;

/**
 * 舰船装配详情
 *
 * @author Leojan
 * @date 2023-10-27 14:31
 */
@Data
@Tag(name = "舰船装配详情")
public class Fitting {

    private String description;

    private List<FittingItem> items;

    private String name;

    @JsonProperty("ship_type_id")
    private Integer shipTypeId;
}
