package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysRolePO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRolePO> {
    int updateBatch(List<SysRolePO> list);

    int updateBatchSelective(List<SysRolePO> list);

    int batchInsert(List<SysRolePO> list);

    int insertOrUpdate(SysRolePO record);

    int insertOrUpdateSelective(SysRolePO record);

    List<String> queryRolesByUserId(Integer id);
    // 只保留基础 CRUD
}
