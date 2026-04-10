package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.SysMenuPO;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuPO> {
    int updateBatch(List<SysMenuPO> list);

    int updateBatchSelective(List<SysMenuPO> list);

    int batchInsert(@Param("list") List<SysMenuPO> list);


    int insertOrUpdateSelective(SysMenuPO record);
}
