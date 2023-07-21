package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * 角色权限表
    */
@Schema(title="角色权限表")
@Data
@EqualsAndHashCode(callSuper=true)
@TableName(value = "sys_role_permission")
public class SysRolePermission extends BaseEntity {
    /**
     * 角色id
     */
    @Schema(name="角色id")
    private Integer roleId;

    /**
     * 资源id
     */
    @Schema(name="资源id")
    private Integer permissionId;

    public static final String COL_ROLE_ID = "role_id";

    public static final String COL_PERMISSION_ID = "permission_id";
}