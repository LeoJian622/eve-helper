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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("权限查询服务")
class SysPermissionServiceTest {

    @Resource
    private SysPermissionService sysPermissionService;
    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void refreshPermRolesRules() {
        sysPermissionService.refreshPermRolesRules();

        Map<String, Object> urlPermRolesRules = redisTemplate.opsForHash().entries(GlobalConstants.URL_PERM_ROLES_KEY);
        for (Map.Entry<String, Object> permRoles : urlPermRolesRules.entrySet()){
            System.out.println(permRoles.getKey() + "XXXXXXXXX" + permRoles.getValue());
            List<String> roles = Convert.toList(String.class,permRoles.getValue());
            System.out.println(roles);
        }

    }
}