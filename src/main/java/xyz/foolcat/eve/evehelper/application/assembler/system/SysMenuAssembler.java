package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysMenu;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysMenuPO;

;

/**
 * SysMenu 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysMenuAssembler {

    /**
     * SysMenuPO 转换为 SysMenu
     * @param sysMenuPO
     * @return
     */
    SysMenu po2Entity(SysMenuPO sysMenuPO);

    /**
     * SysMenu 转换为 SysMenuPO
     * @param sysMenu
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
            @Mapping(target = "sort", ignore = true)
    })
    SysMenuPO entity2Po(SysMenu sysMenu);
} 