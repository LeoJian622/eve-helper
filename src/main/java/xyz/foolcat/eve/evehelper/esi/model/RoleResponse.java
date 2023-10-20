package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 角色列表
 *
 * @author Leojan
 * @date 2023-10-20 16:01
 */

@Data
@Tag(name = "角色列表响应体 200 ok")
public class RoleResponse {

    private String[] roles;

    @JsonProperty("roles_at_base")
    private String[] rolesAtBase;

    @JsonProperty("roles_at_hq")
    private String[] rolesAtHq;

    @JsonProperty("roles_at_other")
    private String[] rolesAtOther;
}
