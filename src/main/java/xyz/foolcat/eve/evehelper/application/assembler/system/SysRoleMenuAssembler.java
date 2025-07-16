package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRoleMenu;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRoleMenuPO;

;

/**
 * SysRoleMenu 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysRoleMenuAssembler {

    /**
     * SysRoleMenuPO 转换为 SysRoleMenu
     * @param sysRoleMenuPO
     * @return
     */
    SysRoleMenu po2Entity(SysRoleMenuPO sysRoleMenuPO);

    /**
     * SysRoleMenu 转换为 SysRoleMenuPO
     * @param sysRoleMenu
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
    })
    SysRoleMenuPO entity2Po(SysRoleMenu sysRoleMenu);
} 