package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.mapper.system.SysRoleMenuMapper;

import java.util.List;

import xyz.foolcat.eve.evehelper.domain.system.SysRoleMenu;

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

