package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.domain.service.system.MarketGroupsService;

@ExtendWith({SpringExtension.class})
@SpringBootTest
@DisplayName("市场组获取测试")
class MarketGroupsServiceTest {

    @Autowired
    private MarketGroupsService marketGroupsService;

    @Test
    void selectMarketGroupTree() {
        System.out.println(marketGroupsService.selectMarketGroupTree());
    }
}