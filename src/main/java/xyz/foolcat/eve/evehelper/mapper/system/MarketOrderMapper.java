package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.dto.system.MarketOrderDTO;

import java.util.List;

@Mapper
public interface MarketOrderMapper extends BaseMapper<MarketOrder> {
    int updateBatch(List<MarketOrder> list);

    int updateBatchSelective(List<MarketOrder> list);

    int batchInsert(@Param("list") List<MarketOrder> list);

    /**
     * 查询某个地点的价格
     *
     * @param locationId 建筑ID
     * @param typeId     物品ID
     * @return
     */
    List<MarketOrderDTO> queryPrice(@Param("locationId") Long locationId, @Param("typeId") Integer typeId);
}