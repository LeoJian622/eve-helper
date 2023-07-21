package xyz.foolcat.eve.evehelper.service.thread;

import com.dtflys.forest.exceptions.ForestNetworkException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.service.system.MarketOrderService;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("市场订单测试")
class MarketOrderAsyncServiceTest {

    @Resource
    private EsiApiService esiApiService;

    @Resource
    private MarketOrderService marketOrderService;

    @Resource
    private MarketOrderAsyncService marketOrderAsyncService;


    @Test
    void saveAndUpdateMarketOrder() throws InterruptedException {
        String regionId = "10000002";
        List<Long> ids = new ArrayList<>();
        int i = 1;
        while (true) {
            try {
                List<MarketOrder> marketOrders = esiApiService.readMarketOrder(regionId, i, null);
                ids.addAll(marketOrders.stream().map(MarketOrder::getOrderId).collect(Collectors.toList()));
                marketOrders.forEach(marketOrder -> marketOrder.setRegionId(Long.valueOf(regionId)));
                marketOrderAsyncService.saveAndUpdateMarketOrder(marketOrders);
                i++;
            } catch (ForestNetworkException e) {
                break;
            }
        }
        marketOrderService.deleteNotInIds(ids);
    }
}