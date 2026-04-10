package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRoleMenu;

import java.util.List;

public interface SysRoleMenuRepository {
    int batchInsert(List<SysRoleMenu> list);

    public boolean insertOrUpdate(SysRoleMenu record);

    int insertOrUpdateSelective(SysRoleMenu record);
} 