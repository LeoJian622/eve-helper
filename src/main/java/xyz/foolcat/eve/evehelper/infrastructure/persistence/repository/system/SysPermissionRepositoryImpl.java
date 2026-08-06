package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysPermission;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysPermissionRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.SysPermissionPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysPermissionMapper;

import java.util.List;
/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class SysPermissionRepositoryImpl implements SysPermissionRepository {

    private final SysPermissionMapper sysPermissionMapper;

    private final SysPermissionPoConverter sysPermissionPoConverter;

    @Override
    public int updateBatch(List<SysPermission> list) {
        return sysPermissionMapper.updateBatch(sysPermissionPoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<SysPermission> list) {
        return sysPermissionMapper.updateBatchSelective(sysPermissionPoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<SysPermission> list) {
        return sysPermissionMapper.batchInsert(sysPermissionPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(SysPermission record) {
        return sysPermissionMapper.insertOrUpdate(sysPermissionPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysPermission record) {
        return sysPermissionMapper.insertOrUpdateSelective(sysPermissionPoConverter.domain2Po(record));
    }

    @Override
    public List<SysPermission> listPermRoles() {
        return sysPermissionPoConverter.po2Domain(sysPermissionMapper.listPermRoles());
    }
}