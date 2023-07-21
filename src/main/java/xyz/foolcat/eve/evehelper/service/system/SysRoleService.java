package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.SysRole;
import xyz.foolcat.eve.evehelper.mapper.system.SysRoleMapper;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    
    public int updateBatch(List<SysRole> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchSelective(List<SysRole> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<SysRole> list) {
        return baseMapper.batchInsert(list);
    }

    public List<String> queryRolesByUserId(Integer id) {
        return baseMapper.queryRolesByUserId(id);
    }
}
