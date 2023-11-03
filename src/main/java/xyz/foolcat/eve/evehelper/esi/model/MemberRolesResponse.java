package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 军团成员角色信息
 *
 * @author Leojan
 * @date 2023-10-26 10:42
 */

@Data
@Tag(name = "军团成员角色信息响应体 200 ok")
public class MemberRolesResponse {


    @JsonProperty("character_id")
    private Integer characterId;

    @JsonProperty("grantable_roles")
    private List<String> grantableRoles;

    @JsonProperty("grantable_roles_at_base")
    private List<String> grantableRolesAtBase;

    @JsonProperty("grantable_roles_at_hq")
    private List<String> grantableRolesAtHq;

    @JsonProperty("grantable_roles_at_other")
    private List<String> grantableRolesAtOther;

    private List<String> roles;

    @JsonProperty("roles_at_base")
    private List<String> rolesAtBase;

    @JsonProperty("roles_at_hq")
    private List<String> rolesAtHq;

    @JsonProperty("roles_at_other")
    private List<String> rolesAtOther;
}
