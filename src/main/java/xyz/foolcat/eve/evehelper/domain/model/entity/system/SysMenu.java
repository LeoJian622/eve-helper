package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 菜单管理
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysMenu extends BaseEntity implements Serializable {
    /**
     * 菜单ID
     */
    private Long id;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：0-禁用 1-开启
     */
    private Boolean visible;

    /**
     * 跳转路径
     */
    private String redirect;

    private static final long serialVersionUID = 1L;
} 