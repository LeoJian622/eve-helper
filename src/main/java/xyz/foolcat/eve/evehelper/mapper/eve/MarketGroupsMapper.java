package xyz.foolcat.eve.evehelper.mapper.eve;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.eve.MarketGroups;

@Mapper
public interface MarketGroupsMapper {
    int deleteByPrimaryKey(Integer marketgroupid);

    int insert(MarketGroups record);

    int insertSelective(MarketGroups record);

    MarketGroups selectByPrimaryKey(Integer marketgroupid);

    int updateByPrimaryKeySelective(MarketGroups record);

    int updateByPrimaryKey(MarketGroups record);

    int updateBatch(List<MarketGroups> list);
}