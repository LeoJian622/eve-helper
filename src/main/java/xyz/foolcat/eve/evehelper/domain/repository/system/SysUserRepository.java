package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;

import java.util.List;

/**
 *
 * @author Leojan
 * date 2025-09-05 15:09
 */

public interface SysUserRepository {
    int updateBatch(List<SysUser> list);

    int updateBatchSelective(List<SysUser> list);

    int batchInsert(List<SysUser> list);

    SysUser queryByUsername(String username);

    int insertOrUpdate(SysUser record);

    int insertOrUpdateSelective(SysUser record);

    int insert(SysUser sysUser);
}
