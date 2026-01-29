package xyz.foolcat.eve.evehelper.domain.service.thread;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.service.system.MarketOrderService;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketOrder;

import java.util.List;

/**
 * @author Leojan
 * date 2022-05-31 14:48
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketOrderAsyncService {

    private final MarketOrderService marketOrderService;

    /**
     * 存储市场订单
     *
     * @param marketOrders
     */
    @Async("EisMarketOrderRequestExecutor")
    public void saveAndUpdateMarketOrder(List<MarketOrder> marketOrders) {
        log.debug("开始保存市场订单信息");
        marketOrderService.saveOrUpdateBatch(marketOrders);
    }

}
