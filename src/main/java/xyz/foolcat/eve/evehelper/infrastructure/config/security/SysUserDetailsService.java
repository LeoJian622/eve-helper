package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.service.system.SysRoleService;
import xyz.foolcat.eve.evehelper.domain.service.system.SysUserService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security {@link UserDetailsService} 适配器 - 基础设施层。
 *
 * <p>从 domain 的 {@link SysUserService} 加载用户、{@link SysRoleService} 加载角色,
 * 组装为 {@link SysUserDetails} 提供 UserDetails 契约。将 Spring Security 依赖收拢到
 * infrastructure,消除 domain ({@code SysUserService} / {@code SysUser}) 对 UserDetails 的直接耦合。</p>
 *
 * @author Leojan
 */
@Service
@RequiredArgsConstructor
public class SysUserDetailsService implements UserDetailsService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserService.queryByUsername(username);
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        List<String> roles = sysRoleService.queryRolesByUserId(sysUser.getId());
        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return new SysUserDetails(sysUser, authorities);
    }
}
