package xyz.foolcat.eve.evehelper.service.eve;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import xyz.foolcat.eve.evehelper.domain.eve.MarketGroups;
import java.util.List;
import xyz.foolcat.eve.evehelper.mapper.eve.MarketGroupsMapper;

@Service
public class MarketGroupsService {

    @Resource
    private MarketGroupsMapper marketGroupsMapper;


    public int deleteByPrimaryKey(Integer marketgroupid) {
        return marketGroupsMapper.deleteByPrimaryKey(marketgroupid);
    }


    public int insert(MarketGroups record) {
        return marketGroupsMapper.insert(record);
    }


    public int insertSelective(MarketGroups record) {
        return marketGroupsMapper.insertSelective(record);
    }


    public MarketGroups selectByPrimaryKey(Integer marketgroupid) {
        return marketGroupsMapper.selectByPrimaryKey(marketgroupid);
    }


    public int updateByPrimaryKeySelective(MarketGroups record) {
        return marketGroupsMapper.updateByPrimaryKeySelective(record);
    }


    public int updateByPrimaryKey(MarketGroups record) {
        return marketGroupsMapper.updateByPrimaryKey(record);
    }


    public int updateBatch(List<MarketGroups> list) {
        return marketGroupsMapper.updateBatch(list);
    }

}




