package xyz.foolcat.eve.evehelper.domain.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketGroups;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.MarketGroupsMapper;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.MarketGroupsTreeVO;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class MarketGroupsService extends ServiceImpl<MarketGroupsMapper, MarketGroups> {

    public int deleteByPrimaryKey(Integer marketgroupid) {
        return baseMapper.deleteById(marketgroupid);
    }


    public int insert(MarketGroups record) {
        return baseMapper.insert(record);
    }


    public int insertSelective(MarketGroups record) {
        return baseMapper.insertOrUpdate(record);
    }


    public MarketGroups selectByPrimaryKey(Integer marketgroupid) {
        return baseMapper.selectById(marketgroupid);
    }


    public int updateByPrimaryKeySelective(MarketGroups record) {
        return baseMapper.insertOrUpdate(record);
    }


    public int updateByPrimaryKey(MarketGroups record) {
        return baseMapper.insertOrUpdate(record);
    }


    public int updateBatch(List<MarketGroups> list) {
        return baseMapper.batchInsert(list);
    }

    public List<MarketGroupsTreeVO> selectMarketGroupTree() {
        return baseMapper.selectGroupTree();
    }

    public List<MarketGroupsTreeVO> selectMarketGroupByParent(Integer marketGroupId) {
        return baseMapper.selectChildren(marketGroupId);
    }

    public int batchInsert(List<MarketGroups> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(MarketGroups record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(MarketGroups record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}





