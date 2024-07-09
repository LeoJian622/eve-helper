package xyz.foolcat.eve.evehelper.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.foolcat.eve.evehelper.domain.system.SysUser;

/**
 * 用户信息工具包
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
        if (principal instanceof SysUser) {
            return ((SysUser) principal).getId();
        }
        return ((Long) principal).intValue();
    }

}
