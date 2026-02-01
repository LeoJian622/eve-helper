package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 *
 * @author Leojan
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class SysUserService  implements UserDetailsService {

    private final SysRoleService sysRoleService;

    private final SysUserRepository sysUserRepository;

    public int updateBatch(List<SysUser> list) {
        return sysUserRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<SysUser> list) {
        return sysUserRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<SysUser> list) {
        return sysUserRepository.batchInsert(list);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser userDetails = this.sysUserRepository.queryByUsername(username);

        List<String> roles = sysRoleService.queryRolesByUserId(userDetails.getId());
        List<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        userDetails.setAuthorities(authorities);

        return userDetails;
    }

    /**
     * 通过用户ID加载用户信息
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    public SysUser loadUserById(Integer userId) {
        SysUser userDetails = this.sysUserRepository.queryById(userId);
        if (userDetails == null) {
            throw new UsernameNotFoundException("用户不存在: userId=" + userId);
        }

        List<String> roles = sysRoleService.queryRolesByUserId(userDetails.getId());
        List<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        userDetails.setAuthorities(authorities);

        return userDetails;
    }

    public int insertOrUpdate(SysUser record) {
        return sysUserRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysUser record) {
        return sysUserRepository.insertOrUpdateSelective(record);
    }

    public int insert(SysUser sysUser) {
        return sysUserRepository.insert(sysUser);
    }
}

