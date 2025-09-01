package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintFormula;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintFormulaRepository;

import java.util.List;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class BlueprintFormulaService {

    private final BlueprintFormulaRepository blueprintFormulaRepository;

    public int batchInsert(List<BlueprintFormula> list) {
        return blueprintFormulaRepository.batchInsert(list);
    }

    public int insertOrUpdate(BlueprintFormula record) {
        return blueprintFormulaRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(BlueprintFormula record) {
        return blueprintFormulaRepository.insertOrUpdateSelective(record);
    }
}

