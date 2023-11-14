package xyz.foolcat.eve.evehelper.esi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 服务器状态信息
 *
 * @author Leojan
 * date 2023-11-14 20:07
 */

@Data
@Tag(name = "服务器状态信息 200 ok")
public class StatusResponse {


    private Integer players;

    @JsonProperty("server_version")
    private String serverVersion;

    @JsonProperty("start_time")
    private OffsetDateTime startTime;

    private Boolean vip;
}
