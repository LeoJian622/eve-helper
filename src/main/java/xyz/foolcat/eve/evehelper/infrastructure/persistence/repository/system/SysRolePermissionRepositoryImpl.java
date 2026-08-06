package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRolePermission;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRolePermissionRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.SysRolePermissionPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRolePermissionMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class SysRolePermissionRepositoryImpl implements SysRolePermissionRepository {

    private final SysRolePermissionMapper sysRolePermissionMapper;

    private final SysRolePermissionPoConverter sysRolePermissionPoConverter;

    @Override
    public int batchInsert(List<SysRolePermission> list) {
        return sysRolePermissionMapper.batchInsert(sysRolePermissionPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(SysRolePermission record) {
        return sysRolePermissionMapper.insertOrUpdate(sysRolePermissionPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysRolePermission record) {
        return sysRolePermissionMapper.insertOrUpdateSelective(sysRolePermissionPoConverter.domain2Po(record));
    }
} 