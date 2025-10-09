package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysPermissionAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysPermission;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysPermissionRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysPermissionMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysPermissionRepositoryImpl implements SysPermissionRepository {

    private final SysPermissionMapper sysPermissionMapper;

    private final SysPermissionAssembler sysPermissionAssembler;

    @Override
    public int updateBatch(List<SysPermission> list) {
        return sysPermissionMapper.updateBatch(sysPermissionAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<SysPermission> list) {
        return sysPermissionMapper.updateBatchSelective(sysPermissionAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<SysPermission> list) {
        return sysPermissionMapper.batchInsert(sysPermissionAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(SysPermission record) {
        return sysPermissionMapper.insertOrUpdate(sysPermissionAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysPermission record) {
        return sysPermissionMapper.insertOrUpdateSelective(sysPermissionAssembler.domain2Po(record));
    }

    @Override
    public List<SysPermission> listPermRoles() {
        return sysPermissionAssembler.po2Domain(sysPermissionMapper.listPermRoles());
    }
}