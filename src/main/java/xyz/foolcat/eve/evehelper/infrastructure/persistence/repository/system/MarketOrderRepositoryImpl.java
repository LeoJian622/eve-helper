package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.MarketOrderRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketOrderPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.MarketOrderMapper;
import xyz.foolcat.eve.evehelper.application.dto.response.MarketOrderDTO;
import java.util.List;

@Repository
public class MarketOrderRepositoryImpl implements MarketOrderRepository {
    @Autowired
    private MarketOrderMapper marketOrderMapper;

    @Override
    public int updateBatch(List<MarketOrderPO> list) {
        return marketOrderMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<MarketOrderPO> list) {
        return marketOrderMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<MarketOrderPO> list) {
        return marketOrderMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(MarketOrderPO record) {
        return marketOrderMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(MarketOrderPO record) {
        return marketOrderMapper.insertOrUpdateSelective(record);
    }

    @Override
    public List<MarketOrderDTO> queryPrice(Long locationId, Integer typeId) {
        return marketOrderMapper.queryPrice(locationId, typeId);
    }

    @Override
    public int batchInsertOrUpdate(List<MarketOrderPO> list) {
        return marketOrderMapper.batchInsertOrUpdate(list);
    }
} 