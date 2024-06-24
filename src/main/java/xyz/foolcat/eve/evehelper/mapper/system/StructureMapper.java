package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.Structure;

import java.util.List;

@Mapper
public interface StructureMapper extends BaseMapper<Structure> {
    int updateBatch(List<Structure> list);

    int updateBatchSelective(List<Structure> list);

    int batchInsert(@Param("list") List<Structure> list);

    int insertOrUpdate(Structure record);

    int insertOrUpdateSelective(Structure record);
}