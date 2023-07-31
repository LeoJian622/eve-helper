package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.SysRoleMenu;

import java.util.List;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
    int batchInsert(@Param("list") List<SysRoleMenu> list);

    int insertOrUpdate(SysRoleMenu record);

    int insertOrUpdateSelective(SysRoleMenu record);
}