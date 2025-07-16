package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.HomeLocation;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.JumpClones;

import java.time.OffsetDateTime;

/**
 * 人物克隆信息响应体
 *
 * @author Leojan
 * date 2023-10-24 11:23
 */

@Data
@Tag(name = "人物克隆信息响应体 200 ok")
public class CloneResponse {

    @JsonProperty("home_location")
    private HomeLocation homeLocation;

    @JsonProperty("jump_clones")
    private JumpClones jumpClones;

    @JsonProperty("last_clone_jump_date")
    private OffsetDateTime lastCloneJumpDate;

    @JsonProperty("last_station_change_date")
    private OffsetDateTime lastStationChangeDate;
}
