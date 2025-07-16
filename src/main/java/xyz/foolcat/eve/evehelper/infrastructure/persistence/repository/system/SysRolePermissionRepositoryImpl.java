package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRolePermissionRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRolePermissionPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRolePermissionMapper;
import java.util.List;

@Repository
public class SysRolePermissionRepositoryImpl implements SysRolePermissionRepository {
    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public int batchInsert(List<SysRolePermissionPO> list) {
        return sysRolePermissionMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(SysRolePermissionPO record) {
        return sysRolePermissionMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(SysRolePermissionPO record) {
        return sysRolePermissionMapper.insertOrUpdateSelective(record);
    }
} 