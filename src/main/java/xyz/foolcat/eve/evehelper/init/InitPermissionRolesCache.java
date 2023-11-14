package xyz.foolcat.eve.evehelper.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.service.system.SysPermissionService;

/**
 *
 * 容器启动完成时加载角色权限规则至Redis缓存
 *
 * @author Leojan
 * date 2021-12-28 16:34
 */

@Component
@RequiredArgsConstructor
public class InitPermissionRolesCache implements CommandLineRunner {

    private final SysPermissionService sysPermissionService;

    @Override
    public void run(String... args) throws Exception {
        sysPermissionService.refreshPermRolesRules();
    }
}
