package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.MarketGroups;
import xyz.foolcat.eve.evehelper.vo.MarketGroupsTreeVO;

import java.util.List;

@Mapper
public interface MarketGroupsMapper extends BaseMapper<MarketGroups> {
    int batchInsert(@Param("list") List<MarketGroups> list);

    int insertOrUpdate(MarketGroups record);

    int insertOrUpdateSelective(MarketGroups record);

    List<MarketGroupsTreeVO> selectGroupTree();

    List<MarketGroupsTreeVO> selectChildren(Integer marketgroupid);
}