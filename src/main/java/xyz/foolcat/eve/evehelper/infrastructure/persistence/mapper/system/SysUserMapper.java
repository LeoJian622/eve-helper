package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.SysUser;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysUserPO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserPO> {
    int updateBatch(List<SysUser> list);

    int updateBatchSelective(List<SysUser> list);

    int batchInsert(List<SysUser> sysUsers);

    SysUser queryByUsername(String username);

    int insertOrUpdate(SysUserPO sysUserPO);

    int insertOrUpdateSelective(SysUserPO sysUserPO);
    // 只保留基础 CRUD
}
