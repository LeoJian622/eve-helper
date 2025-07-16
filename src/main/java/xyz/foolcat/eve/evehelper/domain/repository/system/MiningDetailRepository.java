package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;

import java.util.List;

public interface MiningDetailRepository {
    int updateBatch(List<MiningDetail> list);

    int updateBatchSelective(List<MiningDetail> list);

    int batchInsert(List<MiningDetail> list);

    int insertOrUpdate(MiningDetail record);

    int insertOrUpdateSelective(MiningDetail record);
}