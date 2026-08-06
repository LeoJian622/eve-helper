package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintFormula;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintFormulaRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.BlueprintFormulaPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintFormulaMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class BlueprintFormulaRepositoryImpl implements BlueprintFormulaRepository {

    private final BlueprintFormulaMapper blueprintFormulaMapper;
    private final BlueprintFormulaPoConverter blueprintFormulaPoConverter;

    @Override
    public int batchInsert(List<BlueprintFormula> list) {
        return blueprintFormulaMapper.batchInsert(blueprintFormulaPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(BlueprintFormula record) {
        return blueprintFormulaMapper.insertOrUpdate(blueprintFormulaPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(BlueprintFormula record) {
        return blueprintFormulaMapper.insertOrUpdateSelective(blueprintFormulaPoConverter.domain2Po(record));
    }
} 