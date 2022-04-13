package xyz.foolcat.eve.evehelper.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.domain.system.SysUser;
import xyz.foolcat.eve.evehelper.dto.system.UserDTO;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("mapStruc测试")
class UserConveterTest {

    @Resource
    UserConverter userConverter;

    @Test
    void mapper() {
        SysUser a = new SysUser();
        a.setNickname("adfasdf");
        a.setUsername("11111");
        UserDTO registerDTO = userConverter.sysUser2UserDto(a);
        System.out.println(registerDTO);
    }

}