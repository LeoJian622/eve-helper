package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.SysRolePermission;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
    int batchInsert(@Param("list") List<SysRolePermission> list);

    int insertOrUpdate(SysRolePermission record);

    int insertOrUpdateSelective(SysRolePermission record);
}