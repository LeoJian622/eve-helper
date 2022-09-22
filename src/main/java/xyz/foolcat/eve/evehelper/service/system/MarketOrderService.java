package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.dto.system.MarketOrderDTO;
import xyz.foolcat.eve.evehelper.mapper.system.MarketOrderMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
@CacheConfig(cacheNames = "MarkerOrderCache")
public class MarketOrderService extends ServiceImpl<MarketOrderMapper, MarketOrder> {

    private final EsiApiService esiApiService;

    public int updateBatch(List<MarketOrder> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<MarketOrder> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<MarketOrder> list) {
        return baseMapper.batchInsert(list);
    }

    /**
     * 删除ID不在列表内的记录
     * @param ids
     */
    public void deleteNotInIds(List<Long> ids) {
        QueryWrapper<MarketOrder> marketOrderQueryWrapper = new QueryWrapper<>();
        marketOrderQueryWrapper.notIn(MarketOrder.COL_ORDER_ID, ids);
        baseMapper.delete(marketOrderQueryWrapper);
    }


    /**
     * 查询物品在某个建筑的订单，
     * @param locationId
     * @param typeId
     */
    @Cacheable
    public List<MarketOrderDTO> querySaleAndBuyPrice(Long locationId, Integer typeId){
        return baseMapper.queryPrice(locationId, typeId);
    }





}

