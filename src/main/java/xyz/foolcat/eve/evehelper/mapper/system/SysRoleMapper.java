package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.SysRole;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    int updateBatch(List<SysRole> list);

    int updateBatchSelective(List<SysRole> list);

    int batchInsert(@Param("list") List<SysRole> list);

    int insertOrUpdate(SysRole record);

    int insertOrUpdateSelective(SysRole record);

    List<String> queryRolesByUserId(Integer id);
}