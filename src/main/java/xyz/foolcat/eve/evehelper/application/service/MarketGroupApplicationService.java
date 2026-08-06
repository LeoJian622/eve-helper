package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.service.system.MarketGroupsService;
import xyz.foolcat.eve.evehelper.domain.model.vo.MarketGroupsTreeVO;

import java.util.List;

/**
 * 市场物品组应用服务
 * 负责市场物品分组查询用例
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketGroupApplicationService {

    private final MarketGroupsService marketGroupsService;

    /**
     * 查询指定父节点下的市场物品分组。
     *
     * @param parent 分组的父节点 ID
     * @return 子分组列表
     */
    public List<MarketGroupsTreeVO> queryMarketGroupTree(Integer parent) {
        return marketGroupsService.selectMarketGroupByParent(parent);
    }
}