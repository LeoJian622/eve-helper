package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@ExtendWith({SpringExtension.class})
@SpringBootTest
@DisplayName("市场组获取测试")
class MarketGroupsServiceTest {

    @Resource
    private MarketGroupsService marketGroupsService;

    @Test
    void selectMarketGroupTree() {
        System.out.println(marketGroupsService.selectMarketGroupTree());
    }
}