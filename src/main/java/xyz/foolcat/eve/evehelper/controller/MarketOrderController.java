package xyz.foolcat.eve.evehelper.controller;

import com.dtflys.forest.exceptions.ForestNetworkException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.foolcat.eve.evehelper.common.result.R;
import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.service.system.MarketGroupsService;
import xyz.foolcat.eve.evehelper.service.system.MarketOrderService;
import xyz.foolcat.eve.evehelper.service.thread.MarketOrderAsyncService;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Leojan
 * @date 2022-04-20 10:24
 */

@RestController
@Slf4j
@RequestMapping("/market/order")
@RequiredArgsConstructor
public class MarketOrderController {

    private final EsiApiService esiApiService;

    private final MarketOrderService marketOrderService;

    private final MarketOrderAsyncService marketOrderAsyncService;


    /**
     * 查询星系订单
     * @param regionId
     * @return
     * @throws ParseException
     */
    @GetMapping("/{regionId}")
    public R saveAndUpdateMarketOrder(@PathVariable String regionId){
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
        return R.success();
    }

    @GetMapping("/{locationId}/{typeId}")
    public R queryLocationIdAndTypeId(@PathVariable String locationId, @PathVariable String typeId){
        List<MarketOrder> orders = marketOrderService.lambdaQuery()
                .eq(MarketOrder::getLocationId, locationId)
                .eq(MarketOrder::getTypeId, typeId)
                .orderByDesc(MarketOrder::getPrice)
                .list();
        return R.success(orders);
    }


}
