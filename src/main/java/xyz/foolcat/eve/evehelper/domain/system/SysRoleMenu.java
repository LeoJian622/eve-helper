package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

/**
    * 角色和菜单关联表
    */
@Schema(title="角色和菜单关联表")
@Data
@EqualsAndHashCode(callSuper=true)
@TableName(value = "sys_role_menu")
public class SysRoleMenu extends BaseEntity  {
    /**
     * 角色ID
     */
    @Schema(name="角色ID")
    private Long roleId;

    /**
     * 菜单ID
     */
    @Schema(name="菜单ID")
    private Long menuId;

    public static final String COL_ROLE_ID = "role_id";

    public static final String COL_MENU_ID = "menu_id";
}