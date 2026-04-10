package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserPO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserPO> {
    int updateBatch(List<SysUserPO> list);

    int updateBatchSelective(List<SysUserPO> list);

    int batchInsert(List<SysUserPO> sysUsers);

    SysUserPO queryByUsername(String username);

    int insertOrUpdateSelective(SysUserPO sysUserPO);
    // 只保留基础 CRUD
}
