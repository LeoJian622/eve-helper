package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.SysMenu;
import xyz.foolcat.eve.evehelper.mapper.system.SysMenuMapper;

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

