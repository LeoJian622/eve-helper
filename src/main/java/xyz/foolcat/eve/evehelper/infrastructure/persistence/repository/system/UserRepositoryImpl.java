package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.UserRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysUserMapper;
import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepository {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public int updateBatch(List<SysUserPO> list) {
        return sysUserMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<SysUserPO> list) {
        return sysUserMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<SysUserPO> list) {
        return sysUserMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(SysUserPO record) {
        return sysUserMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(SysUserPO record) {
        return sysUserMapper.insertOrUpdateSelective(record);
    }

    @Override
    public SysUserPO queryByUsername(String username) {
        return sysUserMapper.queryByUsername(username);
    }
} 