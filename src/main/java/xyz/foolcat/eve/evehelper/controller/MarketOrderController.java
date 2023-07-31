package xyz.foolcat.eve.evehelper.controller;

import com.dtflys.forest.exceptions.ForestNetworkException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.foolcat.eve.evehelper.common.result.Result;
import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.service.system.MarketOrderService;
import xyz.foolcat.eve.evehelper.service.thread.MarketOrderAsyncService;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Leojan
 * @date 2022-04-20 10:24
 */

@Tag(name ="市场订单")
@RestController
@Slf4j
@RequestMapping("/*/market/order")
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
    @Parameter(name = "regionId", description = "星系ID", required = true)
    @Operation(summary = "市场订单-订单读取")
    @GetMapping("/{regionId}")
    public Result saveAndUpdateMarketOrder(@PathVariable String regionId) {
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
        return Result.success();
    }

    @Parameters({
            @Parameter(name = "locationId", description = "星系ID" ,required = true),
            @Parameter(name = "typeId", description = "物品ID" ,required = true)
    })
    @Operation(summary = "市场订单-订单查询")
    @GetMapping("/{locationId}/{typeId}")
    public Result queryLocationIdAndTypeId(@PathVariable String locationId, @PathVariable String typeId) {
        List<MarketOrder> orders = marketOrderService.lambdaQuery()
                .eq(MarketOrder::getLocationId, locationId)
                .eq(MarketOrder::getTypeId, typeId)
                .orderByDesc(MarketOrder::getPrice)
                .list();
        return Result.success(orders);
    }


}
