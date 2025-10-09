package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserRolePO;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRolePO> {
    int updateBatch(List<SysUserRolePO> list);

    int updateBatchSelective(List<SysUserRolePO> list);

    int batchInsert(List<SysUserRolePO> list);

    int insertOrUpdate(SysUserRolePO record);

    int insertOrUpdateSelective(SysUserRolePO record);
    // 只保留基础 CRUD
}
