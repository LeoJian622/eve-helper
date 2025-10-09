package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUserRole;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserRolePO;

import java.util.List;

;

/**
 * SysUserRole 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysUserRoleAssembler {

    /**
     * SysUserRolePO 转换为 SysUserRole
     * @param sysUserRolePO
     * @return
     */
    SysUserRole po2Domain(SysUserRolePO sysUserRolePO);

    /**
     * SysUserRole 转换为 SysUserRolePO
     * @param sysUserRole
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
    })
    SysUserRolePO domain2Po(SysUserRole sysUserRole);

    /**
     * SysUserRolePO 转换为 SysUserRole
     * @param sysUserRolePO
     * @return
     */
    List<SysUserRole> po2Domain(List<SysUserRolePO> sysUserRolePO);

    /**
     * SysUserRole 转换为 SysUserRolePO
     * @param sysUserRole
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
    })
    List<SysUserRolePO> domain2Po(List<SysUserRole> sysUserRole);
} 