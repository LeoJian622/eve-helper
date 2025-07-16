package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysPermission;

import java.util.List;

public interface SysPermissionRepository {
    int updateBatch(List<SysPermission> list);

    int updateBatchSelective(List<SysPermission> list);

    int batchInsert(List<SysPermission> list);

    int insertOrUpdate(SysPermission record);

    int insertOrUpdateSelective(SysPermission record);

    List<SysPermission> listPermRoles();
} 