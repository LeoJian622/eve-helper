package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.SysPermission;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    int updateBatch(List<SysPermission> list);

    int updateBatchSelective(List<SysPermission> list);

    int batchInsert(@Param("list") List<SysPermission> list);

    int insertOrUpdate(SysPermission record);

    int insertOrUpdateSelective(SysPermission record);

    List<SysPermission> listPermRoles();
}