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

    /**
     * 未认证或无法识别的主体
     */
    private static final int UNAUTHENTICATED = -1;

    /**
     * 获取当前用户ID。
     * 未认证、匿名或主体类型无法识别时返回 -1，由调用方决定拒绝或放行，
     * 不抛出类型转换异常（否则授权拒绝会退化为 500）。
     *
     * @return 用户ID，未认证时为 -1
     */
    public static Integer getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return UNAUTHENTICATED;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SysUser user) {
            return user.getId() == null ? UNAUTHENTICATED : user.getId();
        }
        // JWT 认证时主体为 userId claim（Number），匿名访问时为字符串
        if (principal instanceof Number number) {
            return number.intValue();
        }
        return UNAUTHENTICATED;
    }
}