package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUserRole;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRoleRepository;

import java.util.List;

/**
 * @author Leojan
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class SysUserRoleService  {

    private final SysUserRoleRepository sysUserRoleRepository;

    public int updateBatch(List<SysUserRole> list) {
        return sysUserRoleRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<SysUserRole> list) {
        return sysUserRoleRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<SysUserRole> list) {
        return sysUserRoleRepository.batchInsert(list);
    }

    public int insertOrUpdate(SysUserRole record) {
        return sysUserRoleRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysUserRole record) {
        return sysUserRoleRepository.insertOrUpdateSelective(record);
    }
}

