package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.SysUser;
import xyz.foolcat.eve.evehelper.mapper.system.SysUserMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 *
 * @author Leojan
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> implements UserDetailsService {

    @Resource
    private SysRoleService sysRoleService;

    public int updateBatch(List<SysUser> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<SysUser> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<SysUser> list) {
        return baseMapper.batchInsert(list);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser userDetails = this.baseMapper.queryByUsername(username);


        List<String> roles = sysRoleService.queryRolesByUserId(userDetails.getId());
        List<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        userDetails.setAuthorities(authorities);

        return userDetails;
    }

    public int insertOrUpdate(SysUser record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysUser record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

