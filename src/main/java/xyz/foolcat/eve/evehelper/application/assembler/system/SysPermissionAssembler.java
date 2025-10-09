package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysPermission;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysPermissionPO;

import java.util.List;


/**
 * SysPermission 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysPermissionAssembler {

    /**
     * SysPermissionPO 转换为 SysPermission
     * @param sysPermissionPO
     * @return
     */
    SysPermission po2Domain(SysPermissionPO sysPermissionPO);

    /**
     * SysPermission 转换为 SysPermissionPO
     * @param sysPermission
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true),
    })
    SysPermissionPO domain2Po(SysPermission sysPermission);

    /**
     * SysPermissionPO列表 转换为 SysPermission列表
     * @param sysPermissionPOList
     * @return
     */
    List<SysPermission> po2Domain(List<SysPermissionPO> sysPermissionPOList);

    /**
     * SysPermission列表 转换为 SysPermissionPO列表
     * @param sysPermissionList
     * @return
     */
    List<SysPermissionPO> domain2Po(List<SysPermission> sysPermissionList);
} 