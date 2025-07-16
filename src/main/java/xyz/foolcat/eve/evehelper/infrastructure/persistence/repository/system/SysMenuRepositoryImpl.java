package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysMenu;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysMenuRepository;

import java.util.List;

/**
 * @author yongj
 * date 2025-07-10 15:11
 */

public class SysMenuRepositoryImpl implements SysMenuRepository {
    @Override
    public int updateBatch(List<SysMenu> list) {
        return 0;
    }

    @Override
    public int updateBatchSelective(List<SysMenu> list) {
        return 0;
    }

    @Override
    public int batchInsert(List<SysMenu> list) {
        return 0;
    }

    @Override
    public int insertOrUpdate(SysMenu record) {
        return 0;
    }

    @Override
    public int insertOrUpdateSelective(SysMenu record) {
        return 0;
    }

    @Override
    public int batchInsertOrUpdate(List<SysMenu> list) {
        return 0;
    }
}
