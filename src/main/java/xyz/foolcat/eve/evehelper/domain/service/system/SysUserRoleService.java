package xyz.foolcat.eve.evehelper.domain.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUserRole;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysUserRoleMapper;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class SysUserRoleService extends ServiceImpl<SysUserRoleMapper, SysUserRole> {


    public int updateBatch(List<SysUserRole> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<SysUserRole> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<SysUserRole> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(SysUserRole record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysUserRole record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

