package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysPermissionRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysPermissionPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysPermissionMapper;

import java.util.List;

@Repository
public class SysPermissionRepositoryImpl implements SysPermissionRepository {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Override
    public int updateBatch(List<SysPermissionPO> list) {
        return 0;
    }

    @Override
    public int updateBatchSelective(List<SysPermissionPO> list) {
        return 0;
    }

    @Override
    public int batchInsert(List<SysPermissionPO> list) {
        return 0;
    }

    @Override
    public int insertOrUpdate(SysPermissionPO record) {
        return 0;
    }

    @Override
    public int insertOrUpdateSelective(SysPermissionPO record) {
        return 0;
    }

    @Override
    public List<SysPermissionPO> listPermRoles() {
        return List.of();
    }
}