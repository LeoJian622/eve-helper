package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserPO;

;

/**
 * SysUser 实体转换器
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysUserAssembler {

    /**
     * SysUserPO 转换为 SysUser
     *
     * @param sysUserPO
     * @return
     */
    @Mappings({
            @Mapping(source = "gmtModified", target = "lastLoginTime")
    })
    SysUser po2Entity(SysUserPO sysUserPO);

    /**
     * SysUser 转换为 SysUserPO
     *
     * @param sysUser
     * @return
     */
    @Mappings({
            @Mapping(source = "lastLoginTime", target = "gmtModified")
    })
    SysUserPO entity2Po(SysUser sysUser);
} 