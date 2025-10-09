package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRole;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRolePO;

import java.util.List;

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
    SysRole po2Domian(SysRolePO sysRolePO);

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
    SysRolePO domian2Po(SysRole sysRole);

    /**
     * SysRolePO 转换为 SysRole
     * @param sysRolePO
     * @return
     */
    List<SysRole> po2Domian(List<SysRolePO> sysRolePO);

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
    List<SysRolePO> domian2Po(List<SysRole> sysRole);
} 