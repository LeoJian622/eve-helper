package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysRoleAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRole;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRoleRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRoleMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysRoleRepositoryImpl implements SysRoleRepository {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleAssembler sysRoleAssembler;

    @Override
    public int updateBatch(List<SysRole> list) {
        return sysRoleMapper.updateBatch(sysRoleAssembler.domian2Po(list));
    }

    @Override
    public int updateBatchSelective(List<SysRole> list) {
        return sysRoleMapper.updateBatchSelective(sysRoleAssembler.domian2Po(list));
    }

    @Override
    public int batchInsert(List<SysRole> list) {
        return sysRoleMapper.batchInsert(sysRoleAssembler.domian2Po(list));
    }

    @Override
    public int insertOrUpdate(SysRole record) {
        return sysRoleMapper.insertOrUpdate(sysRoleAssembler.domian2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysRole record) {
        return sysRoleMapper.insertOrUpdateSelective(sysRoleAssembler.domian2Po(record));
    }

    @Override
    public List<String> queryRolesByUserId(Integer id) {
        return sysRoleMapper.queryRolesByUserId(id);
    }
    // TODO: 实现 BaseRepository<SysRole, Long> 的方法
} 