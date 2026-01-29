package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.application.dto.response.MarketOrderDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketOrder;

import java.util.List;

/**
 * @author Leojan
 */
public interface MarketOrderRepository {
    int updateBatch(List<MarketOrder> list);

    int updateBatchSelective(List<MarketOrder> list);

    int batchInsert(List<MarketOrder> list);

    int insertOrUpdate(MarketOrder record);

    int insertOrUpdateSelective(MarketOrder record);

    List<MarketOrderDTO> queryPrice(Long locationId, Integer typeId);

    int batchInsertOrUpdate(List<MarketOrder> list);

    int deleteColumValueNotIn(String colum, List<Long> ids);

}