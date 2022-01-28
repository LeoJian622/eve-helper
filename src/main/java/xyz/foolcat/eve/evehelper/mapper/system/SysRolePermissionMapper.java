package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.SysRolePermission;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
    int batchInsert(@Param("list") List<SysRolePermission> list);
}