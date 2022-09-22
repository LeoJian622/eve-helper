package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.system.BlueprintsData;
import xyz.foolcat.eve.evehelper.dto.system.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.mapper.system.BlueprintsDataMapper;

import java.util.List;

@Service
@CacheConfig(cacheNames = "BlueprintCostCache")
public class BlueprintsDataService extends ServiceImpl<BlueprintsDataMapper, BlueprintsData> {


    public int batchInsert(List<BlueprintsData> list) {
        return baseMapper.batchInsert(list);
    }

    @Cacheable
    public List<BlueprintCostDTO> queryAllBlueprintsCost() {
        return baseMapper.calcluateCost(null);
    }

    @Cacheable
    public BlueprintCostDTO queryAllBlueprintsCostByBlueTypeId(Integer blueTypeId) {
        List<BlueprintCostDTO> result = baseMapper.calcluateCost(blueTypeId);
        return result.size() > 0 ? result.get(0) : new BlueprintCostDTO();
    }


}
