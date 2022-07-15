package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.mapper.system.SysUserRoleMapper;
import java.util.List;
import xyz.foolcat.eve.evehelper.domain.system.SysUserRole;
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
}
