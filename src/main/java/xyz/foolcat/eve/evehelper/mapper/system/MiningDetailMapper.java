package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.MiningDetail;

import java.util.List;

@Mapper
public interface MiningDetailMapper extends BaseMapper<MiningDetail> {
    int updateBatch(List<MiningDetail> list);

    int updateBatchSelective(List<MiningDetail> list);

    int batchInsert(@Param("list") List<MiningDetail> list);

    int insertOrUpdate(MiningDetail record);

    int insertOrUpdateSelective(MiningDetail record);
}