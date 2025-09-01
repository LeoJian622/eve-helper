package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintsDataAssembler;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintFormulaDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintsData;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsDataRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsDataMapper;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class BlueprintsDataRepositoryImpl implements BlueprintsDataRepository {

    @Resource
    private BlueprintsDataMapper blueprintsDataMapper;

    @Resource
    private BlueprintsDataAssembler blueprintsDataAssembler;

    @Override
    public int batchInsert(List<BlueprintsData> list) {
        return blueprintsDataMapper.batchInsert(blueprintsDataAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(BlueprintsData record) {
        return blueprintsDataMapper.insertOrUpdate(blueprintsDataAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(BlueprintsData record) {
        return blueprintsDataMapper.insertOrUpdateSelective(blueprintsDataAssembler.domain2Po(record));
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