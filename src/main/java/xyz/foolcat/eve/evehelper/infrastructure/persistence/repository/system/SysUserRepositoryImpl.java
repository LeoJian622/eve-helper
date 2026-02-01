package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysUserAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysUserRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysUserMapper;

import java.util.List;

/**
 *
 * @author Leojan
 * date 2025-09-05 15:10
 */

@Repository
@RequiredArgsConstructor
public class SysUserRepositoryImpl implements SysUserRepository {

    private final SysUserMapper sysUserMapper;

    private final SysUserAssembler sysUserAssembler;

    @Override
    public int updateBatch(List<SysUser> list) {
        return sysUserMapper.updateBatch(sysUserAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<SysUser> list) {
        return sysUserMapper.updateBatchSelective(sysUserAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<SysUser> list) {
        return sysUserMapper.batchInsert(sysUserAssembler.domain2Po(list));
    }

    @Override
    public SysUser queryByUsername(String username) {
        return sysUserMapper.queryByUsername(username);
    }

    @Override
    public int insertOrUpdate(SysUser record) {
        return sysUserMapper.insertOrUpdate(sysUserAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysUser record) {
        return sysUserMapper.insertOrUpdateSelective(sysUserAssembler.domain2Po(record));
    }

    @Override
    public int insert(SysUser record) {
        return sysUserMapper.insert(sysUserAssembler.domain2Po(record));
    }

    @Override
    public SysUser queryById(Integer id) {
        return sysUserAssembler.po2Domain(sysUserMapper.selectById(id));
    }
}