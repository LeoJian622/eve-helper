package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.application.dto.response.MarketOrderDTO;
import xyz.foolcat.eve.evehelper.domain.service.system.MarketOrderService;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("市场订单测试")
class MarketOrderServiceTest {

    @Autowired
    private MarketOrderService marketOrderService;

    @Test
    void deleteNotInIds() {
    }

    @Test
    void querySaleAndBuyPrice() {
        List<MarketOrderDTO> marketOrderDTOS= marketOrderService.querySaleAndBuyPrice(60003760L, 28844);

        System.out.println(marketOrderDTOS);
    }
}