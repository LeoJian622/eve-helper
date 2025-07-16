package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 权限表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysPermission extends BaseEntity implements Serializable {
    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限编码
     */
    private String code;

    /**
     * 权限类型：1-菜单 2-按钮
     */
    private Integer type;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 状态：0-禁用 1-开启
     */
    private Boolean status;

    private static final long serialVersionUID = 1L;
} 