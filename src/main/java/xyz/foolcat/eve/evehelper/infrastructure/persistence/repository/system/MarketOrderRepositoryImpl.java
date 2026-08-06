package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.dto.response.MarketOrderDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketOrder;
import xyz.foolcat.eve.evehelper.domain.repository.system.MarketOrderRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.MarketOrderPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketOrderPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.MarketOrderMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class MarketOrderRepositoryImpl implements MarketOrderRepository {
    
    private final MarketOrderMapper marketOrderMapper;

    private final MarketOrderPoConverter marketOrderPoConverter;

    @Override
    public int updateBatch(List<MarketOrder> list) {
        return marketOrderMapper.updateBatch(marketOrderPoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<MarketOrder> list) {
        return marketOrderMapper.updateBatchSelective(marketOrderPoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<MarketOrder> list) {
        return marketOrderMapper.batchInsert(marketOrderPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(MarketOrder record) {
        return marketOrderMapper.insertOrUpdate(marketOrderPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(MarketOrder record) {
        return marketOrderMapper.insertOrUpdateSelective(marketOrderPoConverter.domain2Po(record));
    }

    @Override
    public List<MarketOrderDTO> queryPrice(Long locationId, Integer typeId) {
        return marketOrderMapper.queryPrice(locationId, typeId);
    }

    @Override
    public int batchInsertOrUpdate(List<MarketOrder> list) {
        return marketOrderMapper.batchInsertOrUpdate(marketOrderPoConverter.domain2Po(list));
    }

    @Override
    public int deleteColumValueNotIn(String colum, List<Long> ids) {
        return marketOrderMapper.delete(new QueryWrapper<MarketOrderPO>().notIn(colum, ids));
    }
}