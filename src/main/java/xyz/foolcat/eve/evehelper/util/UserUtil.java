package xyz.foolcat.eve.evehelper.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户信息工具包
 *
 * @author Leojan
 * @date 2023-07-30 11:21
 */

public class UserUtil {

    public static Integer getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((Long) authentication.getPrincipal()).intValue();
    }

}
