package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;

import java.util.List;

/**
 * @author yongj
 */
public interface AssetsRepository {
    int updateBatch(List<Assets> list);

    int updateBatchSelective(List<Assets> list);

    int batchInsert(List<Assets> list);

    int insertOrUpdate(Assets record);

    int insertOrUpdateSelective(Assets record);

    int batchInsertOrUpdate(List<Assets> list);

    List<Assets> selectAssertsInvtypeUniverse(String id, int pages, int rows);

    void removeBatchByIds(List<Long> removeItemIds);

    List<Assets> findByOwnerId(Integer characterId);
}