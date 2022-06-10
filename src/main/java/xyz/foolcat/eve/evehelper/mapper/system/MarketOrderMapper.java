package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.MarketOrder;
import xyz.foolcat.eve.evehelper.dto.system.MarketOrderDTO;

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
     * @param buy        true:取最高价  false:取最低价
     * @param sale       true:取最高价  false:取最低价
     * @return
     */
    List<MarketOrderDTO> queryPrice(@Param("locationId") Long locationId, @Param("typeId") Integer typeId, @Param("isMaxBuy") boolean buy, @Param("isMaxSale") boolean sale);
}