package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsAggregateVO;

import java.util.List;

/**
 * @author yongj
 */
public interface AssetsRepository {
    int updateBatch(List<Assets> list);

    int updateBatchSelective(List<Assets> list);

    int batchInsert(List<Assets> list);

    public boolean insertOrUpdate(Assets record);

    int insertOrUpdateSelective(Assets record);

    int batchInsertOrUpdate(List<Assets> list);

    List<Assets> selectAssertsInvtypeUniverse(String id, int pages, int rows);

    void removeBatchByIds(List<Long> removeItemIds);

    List<Assets> findByOwnerId(Integer characterId);

    /**
     * 按 ownerId 聚合资产为领域读模型。
     *
     * @param ownerId 角色 ID
     * @return 聚合结果;该 owner 无资产时返回 null
     */
    AssetsAggregateVO acquireAggregateByOwnerId(Integer ownerId);
}