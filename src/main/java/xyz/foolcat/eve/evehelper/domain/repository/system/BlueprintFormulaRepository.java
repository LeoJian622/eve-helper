package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintFormula;

import java.util.List;

public interface BlueprintFormulaRepository {
    int batchInsert(List<BlueprintFormula> list);

    int insertOrUpdate(BlueprintFormula record);

    int insertOrUpdateSelective(BlueprintFormula record);
} 