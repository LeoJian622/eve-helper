package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.SysRoleMenu;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
    int batchInsert(@Param("list") List<SysRoleMenu> list);
}