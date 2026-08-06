package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.List;

public interface BlueprintsRepository {
    int updateBatch(List<Blueprints> list);

    int updateBatchSelective(List<Blueprints> list);

    int batchInsert(List<Blueprints> list);

    public boolean insertOrUpdate(Blueprints record);

    int insertOrUpdateSelective(Blueprints record);

    PageResult<BlueprintsDTO> selectBlueprintsInvtypeUniverse(PageQuery page, String id);
} 