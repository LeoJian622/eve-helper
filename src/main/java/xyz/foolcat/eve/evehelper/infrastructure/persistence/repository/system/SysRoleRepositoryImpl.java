package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRoleRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRolePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRoleMapper;

import java.util.List;

@Repository
public class SysRoleRepositoryImpl implements SysRoleRepository {
    @Autowired
    private SysRoleMapper sysRoleMapper;

    public int updateBatch(List<SysRolePO> list) {
        return sysRoleMapper.updateBatch(list);
    }
    public int updateBatchSelective(List<SysRolePO> list) {
        return sysRoleMapper.updateBatchSelective(list);
    }
    public int batchInsert(List<SysRolePO> list) {
        return sysRoleMapper.batchInsert(list);
    }
    public int insertOrUpdate(SysRolePO record) {
        return sysRoleMapper.insertOrUpdate(record);
    }
    public int insertOrUpdateSelective(SysRolePO record) {
        return sysRoleMapper.insertOrUpdateSelective(record);
    }
    public List<String> queryRolesByUserId(Integer id) {
        return sysRoleMapper.queryRolesByUserId(id);
    }
    // TODO: 实现 BaseRepository<SysRole, Long> 的方法
} 