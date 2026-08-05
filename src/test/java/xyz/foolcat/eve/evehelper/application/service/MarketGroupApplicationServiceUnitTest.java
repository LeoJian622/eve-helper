package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.service.system.MarketGroupsService;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.MarketGroupsTreeVO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 市场物品组应用服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("市场物品组应用服务单元测试")
class MarketGroupApplicationServiceUnitTest {

    @Mock
    MarketGroupsService marketGroupsService;

    private MarketGroupApplicationService marketGroupApplicationService;

    @BeforeEach
    void setUp() {
        marketGroupApplicationService = new MarketGroupApplicationService(marketGroupsService);
    }

    @Test
    @DisplayName("查询子分组 -> 返回领域服务结果")
    void queryMarketGroupTree_returns() {
        MarketGroupsTreeVO vo = new MarketGroupsTreeVO();
        when(marketGroupsService.selectMarketGroupByParent(1)).thenReturn(List.of(vo));

        List<MarketGroupsTreeVO> result = marketGroupApplicationService.queryMarketGroupTree(1);

        assertEquals(1, result.size());
    }
}