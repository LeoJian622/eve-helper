package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketGroups;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.MarketGroupsTreeVO;

import java.util.List;

public interface MarketGroupsRepository {
    int batchInsert(List<MarketGroups> list);

    int insertOrUpdate(MarketGroups record);

    int insertOrUpdateSelective(MarketGroups record);

    List<MarketGroupsTreeVO> selectGroupTree();

    List<MarketGroupsTreeVO> selectChildren(Integer marketgroupid);

    int deleteById(Integer marketgroupid);

    int insert(MarketGroups record);

    MarketGroups selectById(Integer marketgroupid);
}