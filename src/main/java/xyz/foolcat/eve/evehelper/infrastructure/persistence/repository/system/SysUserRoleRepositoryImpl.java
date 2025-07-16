package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRoleRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserRolePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysUserRoleMapper;
import java.util.List;

@Repository
public class SysUserRoleRepositoryImpl implements SysUserRoleRepository {
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public int updateBatch(List<SysUserRolePO> list) {
        return sysUserRoleMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<SysUserRolePO> list) {
        return sysUserRoleMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<SysUserRolePO> list) {
        return sysUserRoleMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(SysUserRolePO record) {
        return sysUserRoleMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(SysUserRolePO record) {
        return sysUserRoleMapper.insertOrUpdateSelective(record);
    }
} 