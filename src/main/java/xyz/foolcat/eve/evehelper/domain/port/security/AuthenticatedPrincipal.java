package xyz.foolcat.eve.evehelper.domain.port.security;

/**
 * 已认证主体端口 - 解耦领域层对 Spring Security principal 类型的依赖。
 *
 * <p>认证适配器(infrastructure 层,如包装 SysUser 的 UserDetails 实现)实现此端口,
 * 领域层(如 {@code UserUtil})通过它提取用户标识,无需 import Spring Security 类型,
 * 从而保持 domain 层的框架无关性。</p>
 *
 * @author Leojan
 */
public interface AuthenticatedPrincipal {

    /**
     * 获取已认证用户的系统用户ID。
     *
     * @return 用户ID,主体无法识别时返回 null
     */
    Integer getUserId();
}
