package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintsData;

import java.util.List;

public interface BlueprintsDataRepository {

    int batchInsert(List<BlueprintsData> list);

    public boolean insertOrUpdate(BlueprintsData record);

    int insertOrUpdateSelective(BlueprintsData record);

    List<BlueprintCostDTO> calcluateCost(Integer typeId);
}