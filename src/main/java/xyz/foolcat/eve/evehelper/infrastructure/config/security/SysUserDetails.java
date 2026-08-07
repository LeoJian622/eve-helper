package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.port.security.AuthenticatedPrincipal;

import java.util.Collection;
import java.util.Collections;

/**
 * SysUser 的 Spring Security {@link UserDetails} 适配器。
 *
 * <p>domain 实体 {@link SysUser} 不再直接 implements UserDetails(避免 domain 依赖 Spring Security)。
 * 本适配器位于 infrastructure 层,组合 SysUser 并提供 UserDetails 契约,
 * 供 {@code DaoAuthenticationProvider} 与认证成功处理器使用;同时实现
 * {@link AuthenticatedPrincipal} 端口,使 domain 层的 {@code UserUtil} 能跨适配器提取用户标识。</p>
 *
 * @author Leojan
 */
@Getter
public class SysUserDetails implements UserDetails, AuthenticatedPrincipal {

    private final SysUser sysUser;
    private final Collection<? extends GrantedAuthority> authorities;

    public SysUserDetails(SysUser sysUser, Collection<? extends GrantedAuthority> authorities) {
        this.sysUser = sysUser;
        this.authorities = (authorities == null) ? Collections.emptyList() : authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return sysUser.getPassword();
    }

    @Override
    public String getUsername() {
        return sysUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return sysUser.getStatus() != null && sysUser.getStatus();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return sysUser.getDeleted() != null && sysUser.getDeleted();
    }

    @Override
    public Integer getUserId() {
        return sysUser.getId();
    }
}
