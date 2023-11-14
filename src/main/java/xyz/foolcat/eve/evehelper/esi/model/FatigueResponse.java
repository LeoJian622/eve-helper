package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 *
 * 跳跃疲劳信息
 *
 * @author Leojan
 * date 2023-10-20 14:35
 */
@Data
@Tag(name = "跳跃疲劳信息响应体 200 ok")
public class FatigueResponse {

    @JsonProperty("jump_fatigue_expire_date")
    private OffsetDateTime jumpFatigueExpireDate;

    @JsonProperty("last_jump_date")
    private OffsetDateTime lastJumpDate;

    @JsonProperty("last_update_date")
    private OffsetDateTime lastUpdateDate;
}
