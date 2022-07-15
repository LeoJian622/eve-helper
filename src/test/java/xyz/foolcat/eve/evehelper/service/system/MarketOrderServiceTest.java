package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.RequestMapping;
import xyz.foolcat.eve.evehelper.dto.system.MarketOrderDTO;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("市场订单测试")
class MarketOrderServiceTest {

    @Resource
    private MarketOrderService marketOrderService;

    @Test
    void deleteNotInIds() {
    }

    @Test
    void querSaleAndBuyPrice() {
        List<MarketOrderDTO> marketOrderDTOS= marketOrderService.querSaleAndBuyPrice(60003760L, 28844, true, false);

        System.out.println(marketOrderDTOS);
    }
}