package xyz.foolcat.eve.evehelper.service.system;

import cn.hutool.core.convert.Convert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.common.constant.GlobalConstants;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;



@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("权限查询服务")
class SysPermissionServiceTest {

    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Test
    void refreshPermRolesRules() {
        sysPermissionService.refreshPermRolesRules();

        Map<Object, Object> urlPermRolesRules = redisTemplate.opsForHash().entries(GlobalConstants.URL_PERM_ROLES_KEY);
        for (Map.Entry<Object, Object> permRoles : urlPermRolesRules.entrySet()){
            List<String> roles = Convert.toList(String.class,permRoles.getValue());
            System.out.println("roles = " + roles);
        }

    }
}