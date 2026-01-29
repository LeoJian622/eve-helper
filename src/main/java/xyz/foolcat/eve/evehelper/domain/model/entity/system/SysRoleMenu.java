package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 角色菜单关联表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysRoleMenu extends BaseEntity implements Serializable {
    /**
     * 关联ID
     */
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;

    private static final long serialVersionUID = 1L;
} 