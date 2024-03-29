package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.UniverseName;

import java.util.List;

@Mapper
public interface UniverseNameMapper extends BaseMapper<UniverseName> {
    int updateBatch(List<UniverseName> list);

    int updateBatchSelective(List<UniverseName> list);

    int batchInsert(@Param("list") List<UniverseName> list);

    int insertOrUpdate(UniverseName record);

    int insertOrUpdateSelective(UniverseName record);
}