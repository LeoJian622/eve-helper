package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.service.system.MarketGroupsService;
import xyz.foolcat.eve.evehelper.domain.model.vo.MarketGroupsTreeVO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 市场物品组应用服务单元测试。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("市场物品组应用服务单元测试")
class MarketGroupApplicationServiceUnitTest {

    @MockBean
    MarketGroupsService marketGroupsService;

    @Autowired
    private MarketGroupApplicationService marketGroupApplicationService;

    @Test
    @DisplayName("查询子分组 -> 返回领域服务结果")
    void queryMarketGroupTree_returns() {
        MarketGroupsTreeVO vo = new MarketGroupsTreeVO();
        when(marketGroupsService.selectMarketGroupByParent(1)).thenReturn(List.of(vo));

        List<MarketGroupsTreeVO> result = marketGroupApplicationService.queryMarketGroupTree(1);

        assertEquals(1, result.size());
    }
}