package xyz.foolcat.eve.evehelper.domain.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;

/**
 * 用户工具 - 从安全上下文获取当前用户ID
 *
 * @author Leojan
 * date 2023-07-30 11:21
 */

public class UserUtil {

    public static Integer getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return -1;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SysUser user) {
            return user.getId();
        }
        return ((Long) principal).intValue();
    }
}