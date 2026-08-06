package xyz.foolcat.eve.evehelper.application.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.service.security.ResourceOwnershipPolicy;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

/**
 * 访问控制守卫。
 * 统一处理"取当前主体 + ROOT 豁免 + 归属校验 + 拒绝"，
 * 避免各应用服务重复手写而漏掉某个入口（IDOR 防御）。
 *
 * @author Leojan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final ResourceOwnershipPolicy resourceOwnershipPolicy;

    /**
     * 校验当前用户是否有权访问该人物或军团的资源，无权则抛出访问未授权。
     * ROOT 角色（ADMIN）豁免；未认证或主体无法识别一律拒绝（fail-closed）。
     *
     * @param ownerId  人物或军团ID
     * @param resource 资源名称，仅用于审计日志
     * @throws EveHelperException 无权访问时抛出
     */
    public void requireOwnership(String ownerId, String resource) {
        if (isCurrentUserRoot()) {
            return;
        }
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId == null || currentUserId <= 0) {
            log.warn("{}访问越权：未认证或主体无法识别 ownerId={}", resource, ownerId);
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
        if (!resourceOwnershipPolicy.isOwnedBy(currentUserId, ownerId)) {
            log.warn("{}访问越权：所有者不属于该用户 userId={}, ownerId={}", resource, currentUserId, ownerId);
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
    }

    /**
     * 当前认证用户是否为 ROOT 角色（ADMIN）
     */
    public boolean isCurrentUserRoot() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> GlobalConstants.ROOT_ROLE_CODE.equals(a.getAuthority()));
    }
}
