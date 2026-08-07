package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;





@SpringBootTest
@ActiveProfiles("test")
@DisplayName("用户服务测试")
class SysUserServiceTest {

    @Autowired
    SysUserService service;

    @Test
    void loadUserById() {
        SysUser result = service.loadUserById(1);
        System.out.println(result);
    }

}