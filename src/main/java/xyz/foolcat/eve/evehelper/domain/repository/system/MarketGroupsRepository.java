package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketGroups;
import xyz.foolcat.eve.evehelper.domain.model.vo.MarketGroupsTreeVO;

import java.util.List;

/**
 * @author Leojan
 */
public interface MarketGroupsRepository {
    int batchInsert(List<MarketGroups> list);

    public boolean insertOrUpdate(MarketGroups record);

    int insertOrUpdateSelective(MarketGroups record);

    List<MarketGroupsTreeVO> selectGroupTree();

    List<MarketGroupsTreeVO> selectChildren(Integer marketGroupId);

    int deleteById(Integer marketGroupId);

    int insert(MarketGroups record);

    MarketGroups selectById(Integer marketGroupId);
}