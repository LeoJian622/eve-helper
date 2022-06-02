package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dtflys.forest.exceptions.ForestNetworkException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
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
}

