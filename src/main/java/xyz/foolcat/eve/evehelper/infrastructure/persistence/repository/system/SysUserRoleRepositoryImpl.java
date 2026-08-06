package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUserRole;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRoleRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.SysUserRolePoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysUserRoleMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class SysUserRoleRepositoryImpl implements SysUserRoleRepository {

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysUserRolePoConverter sysUserRolePoConverter;

    @Override
    public int updateBatch(List<SysUserRole> list) {
        return sysUserRoleMapper.updateBatch(sysUserRolePoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<SysUserRole> list) {
        return sysUserRoleMapper.updateBatchSelective(sysUserRolePoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<SysUserRole> list) {
        return sysUserRoleMapper.batchInsert(sysUserRolePoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(SysUserRole record) {
        return sysUserRoleMapper.insertOrUpdate(sysUserRolePoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysUserRole record) {
        return sysUserRoleMapper.insertOrUpdateSelective(sysUserRolePoConverter.domain2Po(record));
    }

}