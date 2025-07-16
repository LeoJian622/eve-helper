package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRolePermission;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRolePermissionPO;

;

/**
 * 角色权限转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysRolePermissionAssembler {

    /**
     * SysRolePermissionPO 转换为 SysRolePermission
     * @param sysRolePermissionPO
     * @return
     */
    SysRolePermission po2Domain(SysRolePermissionPO sysRolePermissionPO);

    /**
     * SysRolePermission 转换为 SysRolePermissionPO
     * @param sysRolePermission
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    SysRolePermissionPO domain2Po(SysRolePermission sysRolePermission);
} 