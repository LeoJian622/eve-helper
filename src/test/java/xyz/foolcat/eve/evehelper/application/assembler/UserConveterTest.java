package xyz.foolcat.eve.evehelper.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.application.dto.response.UserDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;


@ActiveProfiles("test")
@SpringBootTest
@DisplayName("mapStruc测试")
class UserConveterTest {

    @Autowired
    SysUserAssembler userAssembler;

    @Test
    void mapper() {
        SysUser a = new SysUser();
        a.setNickname("adfasdf");
        a.setUsername("11111");
        UserDTO registerDTO = userAssembler.sysUser2UserDto(a);
        System.out.println(registerDTO);
    }

}