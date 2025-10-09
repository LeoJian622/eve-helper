package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRole;

import java.util.List;

public interface SysRoleRepository {

    int updateBatch(List<SysRole> list);

    int updateBatchSelective(List<SysRole> list);

    int batchInsert(List<SysRole> list);

    int insertOrUpdate(SysRole record);

    int insertOrUpdateSelective(SysRole record);

    List<String> queryRolesByUserId(Integer id);
}