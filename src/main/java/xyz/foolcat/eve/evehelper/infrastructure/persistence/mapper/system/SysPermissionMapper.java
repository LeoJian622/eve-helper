package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysPermissionPO;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionPO> {
    // 只保留基础 CRUD

    List<SysPermissionPO> listPermRoles();

    int updateBatch(List<SysPermissionPO> sysPermissionPOS);

    int updateBatchSelective(List<SysPermissionPO> sysPermissionPOS);

    int batchInsert(List<SysPermissionPO> sysPermissionPOS);

    int insertOrUpdate(SysPermissionPO sysPermissionPO);

    int insertOrUpdateSelective(SysPermissionPO sysPermissionPO);
}
