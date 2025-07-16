package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.SysRoleMenuRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRoleMenuPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRoleMenuMapper;
import java.util.List;

@Repository
public class SysRoleMenuRepositoryImpl implements SysRoleMenuRepository {
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public int batchInsert(List<SysRoleMenuPO> list) {
        return sysRoleMenuMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(SysRoleMenuPO record) {
        return sysRoleMenuMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(SysRoleMenuPO record) {
        return sysRoleMenuMapper.insertOrUpdateSelective(record);
    }
} 