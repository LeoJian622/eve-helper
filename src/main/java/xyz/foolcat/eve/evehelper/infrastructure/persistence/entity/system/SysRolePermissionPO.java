package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 角色权限表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色权限表")
@Data
@TableName(value = "sys_role_permission")
public class SysRolePermissionPO extends BaseEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 角色id
     */
    @TableField(value = "role_id")
    @Schema(description = "角色id")
    private Long roleId;

    /**
     * 资源id
     */
    @TableField(value = "permission_id")
    @Schema(description = "资源id")
    private Long permissionId;

    private static final long serialVersionUID = 1L;

    public static final String COL_ID = "id";

    public static final String COL_ROLE_ID = "role_id";

    public static final String COL_PERMISSION_ID = "permission_id";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
} 