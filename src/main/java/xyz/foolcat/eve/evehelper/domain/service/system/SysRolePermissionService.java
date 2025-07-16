package xyz.foolcat.eve.evehelper.domain.service.system;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysRolePermission;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysRolePermissionMapper;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class SysRolePermissionService extends ServiceImpl<SysRolePermissionMapper, SysRolePermission> {


    public int batchInsert(List<SysRolePermission> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(SysRolePermission record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysRolePermission record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

