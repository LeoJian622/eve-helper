package xyz.foolcat.eve.evehelper.esi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.util.List;

/**
 * 军团职位详情
 *
 * @author Leojan
 * @date 2023-10-26 14:43
 */

@Data
@Tag(name = "军团职位详情 200 is ok")
public class CorporationTitleResponse {

    @JsonProperty("grantable_roles")
    private List<String> grantableRoles;

    @JsonProperty("grantable_roles_at_base")
    private List<String> grantableRolesAtBase;

    @JsonProperty("grantable_roles_at_hq")
    private List<String> grantableRolesAtHq;

    @JsonProperty("grantable_roles_at_other")
    private List<String> grantableRolesAtOther;

    private String name;

    private List<String> roles;

    @JsonProperty("roles_at_base")
    private List<String> rolesAtBase;

    @JsonProperty("roles_at_hq")
    private List<String> rolesAtHq;

    @JsonProperty("roles_at_other")
    private List<String> rolesAtOther;

    @JsonProperty("title_id")
    private Integer titleId;
}
