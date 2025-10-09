package xyz.foolcat.eve.evehelper.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;

/**
 * ?????????????????????
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
            return ((SysUser) principal).getId().intValue();
        }
        return ((Long) principal).intValue();
    }

}
