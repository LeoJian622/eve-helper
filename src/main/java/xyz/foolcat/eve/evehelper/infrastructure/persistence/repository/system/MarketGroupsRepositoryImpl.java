package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.MarketGroupsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketGroups;
import xyz.foolcat.eve.evehelper.domain.repository.system.MarketGroupsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.MarketGroupsMapper;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.MarketGroupsTreeVO;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MarketGroupsRepositoryImpl implements MarketGroupsRepository {

    private final MarketGroupsMapper marketGroupsMapper;
    private final MarketGroupsAssembler marketGroupsAssembler;

    @Override
    public int batchInsert(List<MarketGroups> list) {
        return marketGroupsMapper.batchInsert(marketGroupsAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(MarketGroups record) {
        return marketGroupsMapper.insertOrUpdate(marketGroupsAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(MarketGroups record) {
        return marketGroupsMapper.insertOrUpdateSelective(marketGroupsAssembler.domain2Po(record));
    }

    @Override
    public List<MarketGroupsTreeVO> selectGroupTree() {
        return marketGroupsMapper.selectGroupTree();
    }

    @Override
    public List<MarketGroupsTreeVO> selectChildren(Integer marketgroupid) {
        return marketGroupsMapper.selectChildren(marketgroupid);
    }

    @Override
    public int deleteById(Integer marketgroupid) {
        return marketGroupsMapper.deleteById(marketgroupid);
    }

    @Override
    public int insert(MarketGroups record) {
        return 0;
    }

    @Override
    public MarketGroups selectById(Integer marketgroupid) {
        return null;
    }
}