package xyz.foolcat.eve.evehelper.domain.repository.system;


import xyz.foolcat.eve.evehelper.domain.model.entity.system.EsiConfig;

import java.util.List;

public interface EsiConfigRepository{
    int updateBatch(List<EsiConfig> list);

    int updateBatchSelective(List<EsiConfig> list);

    int batchInsert(List<EsiConfig> list);

    int insertOrUpdate(EsiConfig record);

    int insertOrUpdateSelective(EsiConfig record);
}