package xyz.foolcat.eve.evehelper.domain.service.system;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRolePermission;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRolePermissionRepository;

import java.util.List;

/**
 * @author Leojan
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class SysRolePermissionService{

    private final SysRolePermissionRepository sysRolePermissionRepository;

    public int batchInsert(List<SysRolePermission> list) {
        return sysRolePermissionRepository.batchInsert(list);
    }

    public int insertOrUpdate(SysRolePermission record) {
        return sysRolePermissionRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysRolePermission record) {
        return sysRolePermissionRepository.insertOrUpdateSelective(record);
    }
}

