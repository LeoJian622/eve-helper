package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUserRole;

import java.util.List;

public interface SysUserRoleRepository {
    int updateBatch(List<SysUserRole> list);

    int updateBatchSelective(List<SysUserRole> list);

    int batchInsert(List<SysUserRole> list);

    int insertOrUpdate(SysUserRole record);

    int insertOrUpdateSelective(SysUserRole record);

}