package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.dto.system.MarketOrderDTO;
import xyz.foolcat.eve.evehelper.mapper.system.MarketOrderMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

@Slf4j
@Service
@RequiredArgsConstructor
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

    public void saveAndUpdateMarketOrder(String type,String id) {

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
     * @param buy
     * @param sale
     */
    List<MarketOrderDTO> querSaleAndBuyPrice(Long locationId, Integer typeId, boolean buy, boolean sale){
        return baseMapper.queryPrice(locationId, typeId, buy, sale);
    }
}

