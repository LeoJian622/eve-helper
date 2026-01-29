package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintFormulaDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintsData;

import java.util.List;

public interface BlueprintsDataRepository {

    int batchInsert(List<BlueprintsData> list);

    int insertOrUpdate(BlueprintsData record);

    int insertOrUpdateSelective(BlueprintsData record);

    List<BlueprintCostDTO> calcluateCost(Integer typeId);

    List<BlueprintFormulaDTO> queryBlueprintFormulaByTypeId(Integer typeId);
} 