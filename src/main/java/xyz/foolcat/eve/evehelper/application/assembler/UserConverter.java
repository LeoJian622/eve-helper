package xyz.foolcat.eve.evehelper.application.assembler;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserPO;

/**
 * @author Leojan
 * date 2022-03-16 17:12
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * SysUser 转换为 UserDTO
     * @param sysUser
     * @return
     */
    UserDTO sysUser2UserDto(SysUserPO sysUser);

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
            @Mapping(target = "authorities",ignore = true),
    })
    SysUserPO userDto2SysUser(UserDTO userDTO);

}