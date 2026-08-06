package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.domain.model.query.BlueprintsPageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.List;

public interface BlueprintsRepository {
    int updateBatch(List<Blueprints> list);

    int updateBatchSelective(List<Blueprints> list);

    int batchInsert(List<Blueprints> list);

    public boolean insertOrUpdate(Blueprints record);

    int insertOrUpdateSelective(Blueprints record);

    /**
     * 按领域查询条件分页查询蓝图（联表物品名称与所有者）
     *
     * @param criteria 查询条件
     * @return 分页结果
     */
    PageResult<BlueprintsDTO> selectBlueprintsInvtypeUniverse(BlueprintsPageCriteria criteria);
} 