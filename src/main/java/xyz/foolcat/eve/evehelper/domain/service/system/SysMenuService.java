package xyz.foolcat.eve.evehelper.domain.service.system;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysMenu;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.SysMenuMapper;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {


    public int updateBatch(List<SysMenu> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<SysMenu> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<SysMenu> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(SysMenu record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(SysMenu record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

