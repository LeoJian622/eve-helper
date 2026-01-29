package xyz.foolcat.eve.evehelper.infrastructure.external.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 军团成员角色变更记录
 *
 * @author Leojan
 * date 2023-10-26 11:12
 */

@Data
@Tag(name = "军团成员角色变更记录 200 ok")
public class RoleChangeResponse {

    @JsonProperty("changed_at")
    private OffsetDateTime changedAt;

    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("issuer_id")
    private Integer issuerId;

    @JsonProperty("new_roles")
    private List<String> newRoles;

    @JsonProperty("old_roles")
    private List<String> oldRoles;

    @JsonProperty("role_type")
    private String roleType;
}
