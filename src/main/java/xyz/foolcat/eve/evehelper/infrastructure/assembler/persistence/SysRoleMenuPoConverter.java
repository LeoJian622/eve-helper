package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRoleMenu;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRoleMenuPO;

import java.util.List;

/**
 * SysRoleMenu 领域实体与 SysRoleMenuPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysRoleMenuPoConverter {

    /**
     * SysRoleMenuPO 转换为 SysRoleMenu
     *
     * @param sysRoleMenuPO
     * @return
     */
    SysRoleMenu po2Domain(SysRoleMenuPO sysRoleMenuPO);

    /**
     * SysRoleMenu 转换为 SysRoleMenuPO
     *
     * @param sysRoleMenu
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
    })
    SysRoleMenuPO domain2Po(SysRoleMenu sysRoleMenu);

    /**
     * SysRoleMenuPO 列表转换为 SysRoleMenu 列表
     *
     * @param sysRoleMenuPOList
     * @return
     */
    List<SysRoleMenu> po2Domain(List<SysRoleMenuPO> sysRoleMenuPOList);

    /**
     * SysRoleMenu 列表转换为 SysRoleMenuPO 列表
     *
     * @param sysRoleMenuList
     * @return
     */
    List<SysRoleMenuPO> domain2Po(List<SysRoleMenu> sysRoleMenuList);
}
