package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRolePermissionPO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermissionPO> {
    int batchInsert(List<SysRolePermissionPO> sysRolePermissionPOS);

    int insertOrUpdate(SysRolePermissionPO sysRolePermissionPO);

    int insertOrUpdateSelective(SysRolePermissionPO sysRolePermissionPO);
    // 只保留基础 CRUD
}
