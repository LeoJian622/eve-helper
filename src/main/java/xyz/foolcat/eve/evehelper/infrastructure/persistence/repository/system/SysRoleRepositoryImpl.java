package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRole;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRoleRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.SysRolePoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRoleMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class SysRoleRepositoryImpl implements SysRoleRepository {

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePoConverter sysRolePoConverter;

    @Override
    public int updateBatch(List<SysRole> list) {
        return sysRoleMapper.updateBatch(sysRolePoConverter.domian2Po(list));
    }

    @Override
    public int updateBatchSelective(List<SysRole> list) {
        return sysRoleMapper.updateBatchSelective(sysRolePoConverter.domian2Po(list));
    }

    @Override
    public int batchInsert(List<SysRole> list) {
        return sysRoleMapper.batchInsert(sysRolePoConverter.domian2Po(list));
    }

    @Override
    public boolean insertOrUpdate(SysRole record) {
        return sysRoleMapper.insertOrUpdate(sysRolePoConverter.domian2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysRole record) {
        return sysRoleMapper.insertOrUpdateSelective(sysRolePoConverter.domian2Po(record));
    }

    @Override
    public List<String> queryRolesByUserId(Integer id) {
        return sysRoleMapper.queryRolesByUserId(id);
    }
}