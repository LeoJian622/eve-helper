package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.application.dto.response.MarketOrderDTO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketOrderPO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface MarketOrderMapper extends BaseMapper<MarketOrderPO> {
    
    int updateBatch(List<MarketOrderPO> list);

    int updateBatchSelective(List<MarketOrderPO> list);

    int batchInsert(List<MarketOrderPO> list);

    int insertOrUpdate(MarketOrderPO record);

    int insertOrUpdateSelective(MarketOrderPO record);

    int batchInsertOrUpdate(List<MarketOrderPO> list);

    List<MarketOrderDTO> queryPrice(@Param("locationId") Long locationId, @Param("typeId") Integer typeId);

}