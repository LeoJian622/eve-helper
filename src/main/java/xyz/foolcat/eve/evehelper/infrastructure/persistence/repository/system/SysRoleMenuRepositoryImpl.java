package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRoleMenu;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRoleMenuRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.SysRoleMenuPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRoleMenuMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class SysRoleMenuRepositoryImpl implements SysRoleMenuRepository {

    private final SysRoleMenuMapper sysRoleMenuMapper;

    private final SysRoleMenuPoConverter sysRoleMenuPoConverter;

    @Override
    public int batchInsert(List<SysRoleMenu> list) {
        return sysRoleMenuMapper.batchInsert(sysRoleMenuPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(SysRoleMenu record) {
        return sysRoleMenuMapper.insertOrUpdate(sysRoleMenuPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(SysRoleMenu record) {
        return sysRoleMenuMapper.insertOrUpdateSelective(sysRoleMenuPoConverter.domain2Po(record));
    }
}