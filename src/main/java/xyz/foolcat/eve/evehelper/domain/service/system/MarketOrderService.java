package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.dto.response.MarketOrderDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketOrder;
import xyz.foolcat.eve.evehelper.domain.repository.system.MarketOrderRepository;
import xyz.foolcat.eve.evehelper.domain.service.esi.EsiApiService;

import java.util.List;

/**
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
@CacheConfig(cacheNames = "MarkerOrderCache")
public class MarketOrderService {

    private final EsiApiService esiApiService;

    private final MarketOrderRepository marketOrderRepository;

    public int updateBatch(List<MarketOrder> list) {
        return marketOrderRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<MarketOrder> list) {
        return marketOrderRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<MarketOrder> list) {
        return marketOrderRepository.batchInsert(list);
    }

    /**
     * 删除ID不在列表内的记录
     *
     * @param ids
     */
    public int deleteNotInIds(List<Long> ids) {
        return marketOrderRepository.deleteColumValueNotIn(MarketOrder.COL_ORDER_ID,ids);
    }


    /**
     * 查询物品在某个建筑的订单，
     *
     * @param locationId
     * @param typeId
     */
    @Cacheable
    public List<MarketOrderDTO> querySaleAndBuyPrice(Long locationId, Integer typeId) {
        return marketOrderRepository.queryPrice(locationId, typeId);
    }


    public int insertOrUpdate(MarketOrder record) {
        return marketOrderRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(MarketOrder record) {
        return marketOrderRepository.insertOrUpdateSelective(record);
    }

    public int saveOrUpdateBatch(List<MarketOrder> marketOrders) {
        return marketOrderRepository.batchInsertOrUpdate(marketOrders);
    }
}


