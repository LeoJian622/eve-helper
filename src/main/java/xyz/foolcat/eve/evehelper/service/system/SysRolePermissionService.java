package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.SysRolePermission;
import xyz.foolcat.eve.evehelper.mapper.system.SysRolePermissionMapper;
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class SysRolePermissionService extends ServiceImpl<SysRolePermissionMapper, SysRolePermission> {

    
    public int batchInsert(List<SysRolePermission> list) {
        return baseMapper.batchInsert(list);
    }
}
