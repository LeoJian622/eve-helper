package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;
import java.util.List;

/**
 * 权限表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysPermission extends BaseEntity implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 菜单模块ID
     */
    private Long menuId;

    /**
     * URL权限标识
     */
    private String urlPerm;

    /**
     * 按钮权限标识
     */
    private String btnPerm;

    private static final long serialVersionUID = 1L;
} 