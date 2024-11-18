package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

import java.io.Serializable;

/**
 * 角色和菜单关联表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色和菜单关联表")
@Data
@TableName(value = "sys_role_menu")
public class SysRoleMenu extends BaseEntity implements Serializable {
    /**
     * 角色ID
     */
    @TableField(value = "role_id")
    @Schema(description = "角色ID")
    private Long roleId;

    /**
     * 菜单ID
     */
    @TableField(value = "menu_id")
    @Schema(description = "菜单ID")
    private Long menuId;

    private static final long serialVersionUID = 1L;

    public static final String COL_ROLE_ID = "role_id";

    public static final String COL_MENU_ID = "menu_id";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
}