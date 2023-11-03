package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 人物在线状态
 *
 * @author Leojan
 * @date 2023-10-31 15:38
 */

@Data
@Tag(name = "人物在线状态 200 ok")
public class OnlineStatusResponse {

    @JsonProperty("last_login")
    private OffsetDateTime lastLogin;

    @JsonProperty("last_logout")
    private OffsetDateTime lastLogout;

    private Integer logins;

    private Boolean online;
}
