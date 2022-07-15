package xyz.foolcat.eve.evehelper.mapper.system;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.system.MarketGroups;
import xyz.foolcat.eve.evehelper.vo.MarketGroupsTreeVO;

@Mapper
public interface MarketGroupsMapper {
    int deleteByPrimaryKey(Integer marketgroupid);

    int insert(MarketGroups record);

    int insertSelective(MarketGroups record);

    MarketGroups selectByPrimaryKey(Integer marketgroupid);

    int updateByPrimaryKeySelective(MarketGroups record);

    int updateByPrimaryKey(MarketGroups record);

    int updateBatch(List<MarketGroups> list);

    List<MarketGroupsTreeVO> selectGroupTree();

    List<MarketGroupsTreeVO> selectChildren(Integer marketgroupid );
}