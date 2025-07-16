package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.MarketGroupsRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketGroupsPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.MarketGroupsMapper;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.MarketGroupsTreeVO;

import java.util.List;

@Repository
public class MarketGroupsRepositoryImpl implements MarketGroupsRepository {

    @Autowired
    private MarketGroupsMapper marketGroupsMapper;

    @Override
    public int batchInsert(List<MarketGroupsPO> list) {
        return 0;
    }

    @Override
    public int insertOrUpdate(MarketGroupsPO record) {
        return 0;
    }

    @Override
    public int insertOrUpdateSelective(MarketGroupsPO record) {
        return 0;
    }

    @Override
    public List<MarketGroupsTreeVO> selectGroupTree() {
        return List.of();
    }

    @Override
    public List<MarketGroupsTreeVO> selectChildren(Integer marketgroupid) {
        return List.of();
    }
}