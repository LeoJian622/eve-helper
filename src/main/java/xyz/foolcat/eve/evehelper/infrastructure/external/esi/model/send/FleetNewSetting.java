package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 更新舰队登记信息
 *
 * @author Leojan
 * date 2023-10-27 16:14
 */

@Data
@Tag(name = "更新舰队登记信息")
public class FleetNewSetting {

    @JsonProperty("is_free_move")
    private Boolean isFreeMove;

    private String motd;
}
