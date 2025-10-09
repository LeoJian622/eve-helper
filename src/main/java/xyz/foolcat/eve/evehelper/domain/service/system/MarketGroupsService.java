package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketGroups;
import xyz.foolcat.eve.evehelper.domain.repository.system.MarketGroupsRepository;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.MarketGroupsTreeVO;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class MarketGroupsService {

    private final MarketGroupsRepository marketGroupsRepository;

    public int deleteByPrimaryKey(Integer marketgroupid) {
        return marketGroupsRepository.deleteById(marketgroupid);
    }


    public int insert(MarketGroups record) {
        return marketGroupsRepository.insert(record);
    }


    public int insertSelective(MarketGroups record) {
        return marketGroupsRepository.insertOrUpdate(record);
    }


    public MarketGroups selectByPrimaryKey(Integer marketgroupid) {
        return marketGroupsRepository.selectById(marketgroupid);
    }


    public int updateByPrimaryKeySelective(MarketGroups record) {
        return marketGroupsRepository.insertOrUpdate(record);
    }


    public int updateByPrimaryKey(MarketGroups record) {
        return marketGroupsRepository.insertOrUpdate(record);
    }


    public int updateBatch(List<MarketGroups> list) {
        return marketGroupsRepository.batchInsert(list);
    }

    public List<MarketGroupsTreeVO> selectMarketGroupTree() {
        return marketGroupsRepository.selectGroupTree();
    }

    public List<MarketGroupsTreeVO> selectMarketGroupByParent(Integer marketGroupId) {
        return marketGroupsRepository.selectChildren(marketGroupId);
    }

    public int batchInsert(List<MarketGroups> list) {
        return marketGroupsRepository.batchInsert(list);
    }

    public int insertOrUpdate(MarketGroups record) {
        return marketGroupsRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(MarketGroups record) {
        return marketGroupsRepository.insertOrUpdateSelective(record);
    }
}





