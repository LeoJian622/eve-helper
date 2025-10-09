package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysRolePermissionAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRolePermission;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRolePermissionRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRolePermissionMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysRolePermissionRepositoryImpl implements SysRolePermissionRepository {

    private final SysRolePermissionMapper sysRolePermissionMapper;

    private final SysRolePermissionAssembler sysRolePermissionAssembler;

    @Override
    public int batchInsert(List<SysRolePermission> list) {
        return sysRolePermissionMapper.batchInsert(sysRolePermissionAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(SysRolePermission record) {
        return sysRolePermissionMapper.insertOrUpdate(sysRolePermissionAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysRolePermission record) {
        return sysRolePermissionMapper.insertOrUpdateSelective(sysRolePermissionAssembler.domain2Po(record));
    }
} 