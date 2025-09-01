package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintFormulaDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintsData;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsDataRepository;

import java.util.List;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
@CacheConfig(cacheNames = "BlueprintCostCache")
public class BlueprintsDataService {

    private final BlueprintsDataRepository blueprintsDataRepository;

    public int batchInsert(List<BlueprintsData> list) {
        return blueprintsDataRepository.batchInsert(list);
    }

    /**
     * 查询卖单价，收单价，材料卖单总价，材料收单总价
     *
     * @return List<BlueprintCostDTO>
     */
    @Cacheable
    public List<BlueprintCostDTO> queryAllBlueprintsCost() {
        return blueprintsDataRepository.calcluateCost(null);
    }

    /**
     * 根据物品ID查询卖单价，收单价，材料卖单总价，材料收单总价
     *
     * @param typeId 物品ID
     * @return BlueprintCostDTO
     */
    @Cacheable
    public BlueprintCostDTO queryAllBlueprintsCostByBlueTypeId(Integer typeId) {
        List<BlueprintCostDTO> result = blueprintsDataRepository.calcluateCost(typeId);
        return !result.isEmpty() ? result.get(0) : new BlueprintCostDTO();
    }


    public BlueprintFormulaDTO queryBlueprintFormulaByTypeId(Integer typeId) {
        return null;
    }


    public int insertOrUpdate(BlueprintsData record) {
        return blueprintsDataRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(BlueprintsData record) {
        return blueprintsDataRepository.insertOrUpdateSelective(record);
    }
}

