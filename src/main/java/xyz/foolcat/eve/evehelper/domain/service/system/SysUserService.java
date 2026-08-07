package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRepository;

import java.util.List;

/**
 * 用户服务
 *
 * <p>Spring Security {@code UserDetailsService} 契约已迁至 infrastructure 层的
 * {@code SysUserDetailsService},本类仅保留领域查询与持久化编排,不再依赖 Spring Security。</p>
 *
 * @author Leojan
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class SysUserService {

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

    /**
     * 按用户名查询用户(领域查询,不含权限装配)。
     *
     * @param username 用户名
     * @return 用户实体,不存在返回 null
     */
    public SysUser queryByUsername(String username) {
        return sysUserRepository.queryByUsername(username);
    }

    /**
     * 通过用户ID加载用户信息(领域查询,不含权限装配)。
     *
     * @param userId 用户ID
     * @return 用户实体,不存在返回 null
     */
    public SysUser loadUserById(Integer userId) {
        return sysUserRepository.queryById(userId);
    }

    public boolean insertOrUpdate(SysUser record) {
        return sysUserRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysUser record) {
        return sysUserRepository.insertOrUpdateSelective(record);
    }

    public int insert(SysUser sysUser) {
        return sysUserRepository.insert(sysUser);
    }
}
