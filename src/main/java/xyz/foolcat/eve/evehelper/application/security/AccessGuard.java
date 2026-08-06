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
     * ROOT 角色（ADMIN）豁免归属校验，但参数校验对其同样生效；
     * 未认证或主体无法识别一律拒绝（fail-closed）。
     *
     * @param ownerId  人物或军团ID
     * @param resource 资源名称，仅用于审计日志
     * @throws EveHelperException 参数缺失或无权访问时抛出
     */
    public void requireOwnership(String ownerId, String resource) {
        // 参数校验先于 ROOT 豁免，避免 ROOT 携带空值穿透到下游查询
        // "null" 字面量来自调用点的 String.valueOf(null)，同样视为缺失
        if (ownerId == null || ownerId.isBlank() || "null".equals(ownerId)) {
            log.warn("{}访问参数缺失：ownerId 为空", resource);
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
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
     * 校验当前用户是否有权访问该系统用户的资源，无权则抛出访问未授权。
     * 与 {@link #requireOwnership} 的区别：此处比对的是系统用户ID（sys_user.id），
     * 而非 EVE 人物/军团ID，两者不可混用。
     * ROOT 角色（ADMIN）豁免归属校验，但参数校验对其同样生效；
     * 未认证或主体无法识别一律拒绝（fail-closed）。
     *
     * @param userId   目标系统用户ID
     * @param resource 资源名称，仅用于审计日志
     * @throws EveHelperException 参数缺失或无权访问时抛出
     */
    public void requireSelfOrRoot(Integer userId, String resource) {
        // 参数校验先于 ROOT 豁免，避免 ROOT 携带 null 穿透到下游查询
        if (userId == null) {
            log.warn("{}访问参数缺失：userId 为空", resource);
            throw new EveHelperException(ResultCode.PARAM_ERROR);
        }
        if (isCurrentUserRoot()) {
            return;
        }
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId == null || currentUserId <= 0) {
            log.warn("{}访问越权：未认证或主体无法识别 userId={}", resource, userId);
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
        if (!currentUserId.equals(userId)) {
            log.warn("{}访问越权：非本人访问 currentUserId={}, userId={}", resource, currentUserId, userId);
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
    }

    /**
     * 当前认证用户是否为 ROOT 角色（ADMIN）。
     * 要求令牌已通过认证，未认证令牌即便携带 ADMIN 权限也不豁免（fail-closed）。
     */
    public boolean isCurrentUserRoot() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> GlobalConstants.ROOT_ROLE_CODE.equals(a.getAuthority()));
    }
}
