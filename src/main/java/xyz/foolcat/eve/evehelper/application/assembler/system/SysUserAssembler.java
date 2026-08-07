package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;


/**
 * SysUser 实体转换器
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface SysUserAssembler {

    /**
     * SysUser 转换为 UserDTO
     * @param sysUser
     * @return
     */
    UserDTO sysUser2UserDto(SysUser sysUser);

    /**
     * UserDTO转换为SysUser
     * @param userDTO
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate",ignore = true),
            @Mapping(target = "gmtModified",ignore = true),
            @Mapping(target = "id",ignore = true),
            @Mapping(target = "avatar",ignore = true),
            @Mapping(target = "mobile",ignore = true),
            @Mapping(target = "status",ignore = true),
            @Mapping(target = "deleted",ignore = true),
            @Mapping(target = "lastLoginTime",ignore = true),
    })
    SysUser userDto2SysUser(UserDTO userDTO);
}