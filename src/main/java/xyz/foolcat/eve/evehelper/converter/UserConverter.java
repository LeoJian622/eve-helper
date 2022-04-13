package xyz.foolcat.eve.evehelper.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.foolcat.eve.evehelper.domain.system.SysUser;
import xyz.foolcat.eve.evehelper.dto.system.UserDTO;
import xyz.foolcat.eve.evehelper.vo.UserVO;

/**
 * @author Leojan
 * @date 2022-03-16 17:12
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

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
    SysUser userDto2SysUser(UserDTO userDTO);

}