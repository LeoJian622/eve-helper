package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserRoleAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUserRole;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRoleRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysUserRoleMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class SysUserRoleRepositoryImpl implements SysUserRoleRepository {

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysUserRoleAssembler sysUserRoleAssembler;

    @Override
    public int updateBatch(List<SysUserRole> list) {
        return sysUserRoleMapper.updateBatch(sysUserRoleAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<SysUserRole> list) {
        return sysUserRoleMapper.updateBatchSelective(sysUserRoleAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<SysUserRole> list) {
        return sysUserRoleMapper.batchInsert(sysUserRoleAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(SysUserRole record) {
        return sysUserRoleMapper.insertOrUpdate(sysUserRoleAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysUserRole record) {
        return sysUserRoleMapper.insertOrUpdateSelective(sysUserRoleAssembler.domain2Po(record));
    }

}