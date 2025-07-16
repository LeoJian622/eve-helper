package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsDataRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsDataPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsDataMapper;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintFormulaDTO;
import java.util.List;

@Repository
public class BlueprintsDataRepositoryImpl implements BlueprintsDataRepository {
    @Autowired
    private BlueprintsDataMapper blueprintsDataMapper;

    @Override
    public int batchInsert(List<BlueprintsDataPO> list) {
        return blueprintsDataMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(BlueprintsDataPO record) {
        return blueprintsDataMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(BlueprintsDataPO record) {
        return blueprintsDataMapper.insertOrUpdateSelective(record);
    }

    @Override
    public List<BlueprintCostDTO> calcluateCost(Integer typeId) {
        return blueprintsDataMapper.calcluateCost(typeId);
    }

    @Override
    public List<BlueprintFormulaDTO> queryBlueprintFormulaByTypeId(Integer typeId) {
        return blueprintsDataMapper.queryBlueprintFormulaByTypeId(typeId);
    }
} 