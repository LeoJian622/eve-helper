package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintFormulaRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintFormulaPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintFormulaMapper;
import java.util.List;

@Repository
public class BlueprintFormulaRepositoryImpl implements BlueprintFormulaRepository {
    @Autowired
    private BlueprintFormulaMapper blueprintFormulaMapper;

    @Override
    public int batchInsert(List<BlueprintFormulaPO> list) {
        return blueprintFormulaMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(BlueprintFormulaPO record) {
        return blueprintFormulaMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(BlueprintFormulaPO record) {
        return blueprintFormulaMapper.insertOrUpdateSelective(record);
    }
} 