package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EsiConfigPO;

import java.util.List;

@Mapper
public interface EsiConfigMapper extends BaseMapper<EsiConfigPO> {
    int updateBatch(List<EsiConfigPO> esiConfigPOS);

    int updateBatchSelective(List<EsiConfigPO> esiConfigPOS);

    int batchInsert(List<EsiConfigPO> esiConfigPOS);

    int insertOrUpdate(EsiConfigPO esiConfigPO);

    int insertOrUpdateSelective(EsiConfigPO esiConfigPO);
    // 只保留基础 CRUD
}
