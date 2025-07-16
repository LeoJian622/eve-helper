package xyz.foolcat.eve.evehelper.domain.service.system;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRoleMenu;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRoleMenuMapper;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class SysRoleMenuService extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> {


    public int batchInsert(List<SysRoleMenu> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(SysRoleMenu record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysRoleMenu record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

