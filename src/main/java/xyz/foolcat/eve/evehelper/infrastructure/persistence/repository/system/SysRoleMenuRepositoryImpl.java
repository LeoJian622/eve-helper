package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.SysRoleMenuAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRoleMenu;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRoleMenuRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRoleMenuMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysRoleMenuRepositoryImpl implements SysRoleMenuRepository {

    private final SysRoleMenuMapper sysRoleMenuMapper;

    private final SysRoleMenuAssembler sysRoleMenuAssembler;

    @Override
    public int batchInsert(List<SysRoleMenu> list) {
        return sysRoleMenuMapper.batchInsert(sysRoleMenuAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(SysRoleMenu record) {
        return sysRoleMenuMapper.insertOrUpdate(sysRoleMenuAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysRoleMenu record) {
        return sysRoleMenuMapper.insertOrUpdateSelective(sysRoleMenuAssembler.domain2Po(record));
    }
}