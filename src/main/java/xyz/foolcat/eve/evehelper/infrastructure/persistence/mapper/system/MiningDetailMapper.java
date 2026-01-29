package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MiningDetailPO;

import java.util.List;

@Mapper
public interface MiningDetailMapper extends BaseMapper<MiningDetailPO> {
    int updateBatch(List<MiningDetailPO> list);

    int updateBatchSelective(List<MiningDetailPO> list);

    int batchInsert(List<MiningDetailPO> list);

    int insertOrUpdate(MiningDetailPO record);

    int insertOrUpdateSelective(MiningDetailPO record);

    int batchInsertOrUpdate(List<MiningDetailPO> miningDetailPOS);
    // 只保留基础 CRUD
}
