package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRole;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRolePO;

import java.util.List;

/**
 * SysRole 领域实体与 SysRolePO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysRolePoConverter {

    /**
     * SysRolePO 转换为 SysRole
     *
     * @param sysRolePO
     * @return SysRole
     */
    SysRole po2Domian(SysRolePO sysRolePO);

    /**
     * SysRole 转换为 SysRolePO
     *
     * @param sysRole
     * @return SysRolePO
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
            @Mapping(target = "sort", ignore = true),
            @Mapping(target = "deleted", ignore = true)
    })
    SysRolePO domian2Po(SysRole sysRole);

    /**
     * SysRolePO 转换为 SysRole
     *
     * @param sysRolePO
     * @return List<SysRole>
     */
    List<SysRole> po2Domian(List<SysRolePO> sysRolePO);

    /**
     * SysRole 转换为 SysRolePO
     *
     * @param sysRole
     * @return List<SysRolePO>
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
            @Mapping(target = "sort", ignore = true),
            @Mapping(target = "deleted", ignore = true)
    })
    List<SysRolePO> domian2Po(List<SysRole> sysRole);
}
