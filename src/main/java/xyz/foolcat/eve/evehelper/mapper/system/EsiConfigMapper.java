package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.EsiConfig;

@Mapper
public interface EsiConfigMapper extends BaseMapper<EsiConfig> {
    int updateBatch(List<EsiConfig> list);

    int updateBatchSelective(List<EsiConfig> list);

    int batchInsert(@Param("list") List<EsiConfig> list);

    int insertOrUpdate(EsiConfig record);

    int insertOrUpdateSelective(EsiConfig record);
}