package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRolePermission;

import java.util.List;

public interface SysRolePermissionRepository {
    int batchInsert(List<SysRolePermission> list);

    public boolean insertOrUpdate(SysRolePermission record);

    int insertOrUpdateSelective(SysRolePermission record);
} 