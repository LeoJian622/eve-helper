package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketGroupsPO;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.MarketGroupsTreeVO;

import java.util.List;

@Mapper
public interface MarketGroupsMapper extends BaseMapper<MarketGroupsPO> {
    int batchInsert(List<MarketGroupsPO> marketGroupsPOS);

    int insertOrUpdate(MarketGroupsPO marketGroupsPO);

    int insertOrUpdateSelective(MarketGroupsPO marketGroupsPO);

    List<MarketGroupsTreeVO> selectChildren(Integer marketgroupid);

    List<MarketGroupsTreeVO> selectGroupTree();
    // 只保留基础 CRUD
}
