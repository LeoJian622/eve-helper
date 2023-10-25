package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 军团机库容器7天审计记录
 *
 * @author Leojan
 * @date 2023-10-25 15:49
 */
@Data
@Tag(name = "军团机库容器7天审计记录响应体 200 ok")
public class ContainersLogsResponse {

    private String action;

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("container_id")
    private Long containerId;

    @JsonProperty("container_type_id")
    private Integer containerTypeId;

    @JsonProperty("location_flag")
    private String locationFlag;

    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("logged_at")
    private OffsetDateTime loggedAt;

    @JsonProperty("new_config_bitmask")
    private Integer newConfigBitmask;

    @JsonProperty("old_config_bitmask")
    private Integer oldConfigBitmask;

    @JsonProperty("password_type")
    private String passwordType;

    private Integer quantity;

    @JsonProperty("type_id")
    private Integer typeId;
}
