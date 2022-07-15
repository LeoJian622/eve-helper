package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.MarketGroups;
import java.util.List;
import xyz.foolcat.eve.evehelper.mapper.system.MarketGroupsMapper;
import xyz.foolcat.eve.evehelper.vo.MarketGroupsTreeVO;

@Service
@Transactional(rollbackFor = RuntimeException.class)
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

    public List<MarketGroupsTreeVO> selectMarketGroupTree(){
        return marketGroupsMapper.selectGroupTree();
    }

    public List<MarketGroupsTreeVO> selectMarketGroupByParent(Integer marketGroupId){
        return marketGroupsMapper.selectChildren(marketGroupId);
    }

}




