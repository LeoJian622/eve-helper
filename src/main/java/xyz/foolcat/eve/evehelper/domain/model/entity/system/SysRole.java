package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serializable;

/**
 * 角色管理
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysRole extends BaseEntity implements Serializable {
    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态：0-禁用 1-开启
     */
    private Boolean status;

    private static final long serialVersionUID = 1L;
} 