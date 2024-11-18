package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

import java.io.Serializable;

/**
 * 角色权限表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色权限表")
@Data
@TableName(value = "sys_role_permission")
public class SysRolePermission extends BaseEntity implements Serializable {
    /**
     * 角色id
     */
    @TableField(value = "role_id")
    @Schema(description = "角色id")
    private Integer roleId;

    /**
     * 资源id
     */
    @TableField(value = "permission_id")
    @Schema(description = "资源id")
    private Integer permissionId;

    private static final long serialVersionUID = 1L;

    public static final String COL_ROLE_ID = "role_id";

    public static final String COL_PERMISSION_ID = "permission_id";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
}