package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintsData;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsDataRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.BlueprintsDataPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsDataMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class BlueprintsDataRepositoryImpl implements BlueprintsDataRepository {

    private final BlueprintsDataMapper blueprintsDataMapper;
    private final BlueprintsDataPoConverter blueprintsDataPoConverter;

    @Override
    public int batchInsert(List<BlueprintsData> list) {
        return blueprintsDataMapper.batchInsert(blueprintsDataPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(BlueprintsData record) {
        return blueprintsDataMapper.insertOrUpdate(blueprintsDataPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(BlueprintsData record) {
        return blueprintsDataMapper.insertOrUpdateSelective(blueprintsDataPoConverter.domain2Po(record));
    }

    @Override
    public List<BlueprintCostDTO> calcluateCost(Integer typeId) {
        return blueprintsDataMapper.calcluateCost(typeId);
    }
}