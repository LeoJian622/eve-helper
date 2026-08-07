package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserPO;

import java.util.List;

/**
 * SysUser 领域实体与 SysUserPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysUserPoConverter {

    /**
     * SysUserPO 转换为 SysUser
     *
     * @param sysUserPO
     * @return
     */
    @Mappings({
            @Mapping(source = "gmtModified", target = "lastLoginTime")
    })
    SysUser po2Domain(SysUserPO sysUserPO);

    /**
     * SysUser 转换为 SysUserPO
     *
     * @param sysUser
     * @return
     */
    @Mappings({
            @Mapping(source = "lastLoginTime", target = "gmtModified"),
            @Mapping(target = "authorities", ignore = true)
    })
    SysUserPO domain2Po(SysUser sysUser);

    /**
     * SysUserPO 转换为 SysUser
     *
     * @param sysUserPO
     * @return
     */
    @Mappings({
            @Mapping(source = "gmtModified", target = "lastLoginTime")
    })
    List<SysUser> po2Domain(List<SysUserPO> sysUserPO);

    /**
     * SysUser 转换为 SysUserPO
     *
     * @param sysUser
     * @return
     */
    @Mappings({
            @Mapping(source = "lastLoginTime", target = "gmtModified"),
            @Mapping(target = "authorities", ignore = true)
    })
    List<SysUserPO> domain2Po(List<SysUser> sysUser);
}
