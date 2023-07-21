package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.system.BlueprintsData;
import xyz.foolcat.eve.evehelper.dto.system.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.dto.system.BlueprintFormulaDTO;
import xyz.foolcat.eve.evehelper.mapper.system.BlueprintsDataMapper;

import java.util.List;

@Service
@CacheConfig(cacheNames = "BlueprintCostCache")
public class BlueprintsDataService extends ServiceImpl<BlueprintsDataMapper, BlueprintsData> {


    public int batchInsert(List<BlueprintsData> list) {
        return baseMapper.batchInsert(list);
    }

    /**
     * 查询卖单价，收单价，材料卖单总价，材料收单总价
     * @return List<BlueprintCostDTO>
     */
    @Cacheable
    public List<BlueprintCostDTO> queryAllBlueprintsCost() {
        return baseMapper.calcluateCost(null);
    }

    /**
     *  根据物品ID查询卖单价，收单价，材料卖单总价，材料收单总价
     * @param typeId  物品ID
     * @return BlueprintCostDTO
     */
    @Cacheable
    public BlueprintCostDTO queryAllBlueprintsCostByBlueTypeId(Integer typeId) {
        List<BlueprintCostDTO> result = baseMapper.calcluateCost(typeId);
        return result.size() > 0 ? result.get(0) : new BlueprintCostDTO();
    }


    public BlueprintFormulaDTO queryBlueprintFormulaByTypeId(Integer typeId) {
        return null;
    }


}
