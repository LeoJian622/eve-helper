package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintFormulaAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintFormula;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintFormulaRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintFormulaMapper;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class BlueprintFormulaRepositoryImpl implements BlueprintFormulaRepository {

    @Resource
    private BlueprintFormulaMapper blueprintFormulaMapper;

    @Resource
    private BlueprintFormulaAssembler blueprintFormulaAssembler;

    @Override
    public int batchInsert(List<BlueprintFormula> list) {
        return blueprintFormulaMapper.batchInsert(blueprintFormulaAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(BlueprintFormula record) {
        return blueprintFormulaMapper.insertOrUpdate(blueprintFormulaAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(BlueprintFormula record) {
        return blueprintFormulaMapper.insertOrUpdateSelective(blueprintFormulaAssembler.domain2Po(record));
    }
} 