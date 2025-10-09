package xyz.foolcat.eve.evehelper.domain.service.system;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRoleMenu;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system.SysRoleMenuRepositoryImpl;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class SysRoleMenuService  {

    private SysRoleMenuRepositoryImpl sysRoleMenuRepository;
    public int batchInsert(List<SysRoleMenu> list) {
        return sysRoleMenuRepository.batchInsert(list);
    }

    public int insertOrUpdate(SysRoleMenu record) {
        return sysRoleMenuRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysRoleMenu record) {
        return sysRoleMenuRepository.insertOrUpdateSelective(record);
    }
}

