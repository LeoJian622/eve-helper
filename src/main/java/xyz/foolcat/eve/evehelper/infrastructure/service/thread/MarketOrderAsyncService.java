package xyz.foolcat.eve.evehelper.infrastructure.service.thread;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketOrder;
import xyz.foolcat.eve.evehelper.domain.service.system.MarketOrderService;

import java.util.List;

/**
 * 市场订单异步持久化服务 - 基础设施层。
 *
 * <p>原位于 domain/service/thread,因其依赖 {@code @Async} 线程池(技术基础设施关注点)迁至
 * infrastructure,保持 domain 层框架无关。编排 domain 的 {@link MarketOrderService} 完成批量持久化。</p>
 *
 * @author Leojan
 * date 2022-05-31 14:48
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketOrderAsyncService {

    private final MarketOrderService marketOrderService;

    /**
     * 异步存储市场订单
     *
     * @param marketOrders 市场订单列表
     */
    @Async("EsiMarketOrderRequestExecutor")
    public void saveAndUpdateMarketOrder(List<MarketOrder> marketOrders) {
        log.debug("开始保存市场订单信息");
        marketOrderService.saveOrUpdateBatch(marketOrders);
    }
}
