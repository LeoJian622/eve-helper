package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 舰队登记详细信息
 *
 * @author Leojan
 * date 2023-10-27 15:43
 */
@Data
@Tag(name = "舰队登记详细信息 200 ok")
public class FleetDetailResponse {

    @JsonProperty("is_free_move")
    private Boolean isFreeMove;

    @JsonProperty("is_registered")
    private Boolean isRegistered;

    @JsonProperty("is_voice_enabled")
    private Boolean isVoiceEnabled;

    private String motd;
}
