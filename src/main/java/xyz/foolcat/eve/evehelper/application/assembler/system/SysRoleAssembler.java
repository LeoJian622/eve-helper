package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRole;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRolePO;

;

/**
 * SysRole 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysRoleAssembler {

    /**
     * SysRolePO 转换为 SysRole
     * @param sysRolePO
     * @return
     */
    SysRole po2Entity(SysRolePO sysRolePO);

    /**
     * SysRole 转换为 SysRolePO
     * @param sysRole
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
            @Mapping(target = "sort", ignore = true),
            @Mapping(target = "deleted", ignore = true)
    })
    SysRolePO entity2Po(SysRole sysRole);
} 