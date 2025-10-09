package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketOrder;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketOrderPO;

import java.util.List;

/**
 * MarketOrder 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface MarketOrderAssembler {

    /**
     * MarketOrderPO 转换为 MarketOrder
     * @param marketOrderPO 实例对象
     * @return MarketOrder
     */
    MarketOrder po2Domain(MarketOrderPO marketOrderPO);

    /**
     * MarketOrder 转换为 MarketOrderPO
     * @param marketOrder 实例对象
     * @return MarketOrderPO
     */
    MarketOrderPO domain2Po(MarketOrder marketOrder);

    /**
     * MarketOrderPO 转换为 MarketOrder
     * @param marketOrderPO 实例对象
     * @return List<MarketOrder>
     */
    List<MarketOrder> po2Domain(List<MarketOrderPO> marketOrderPO);

    /**
     * MarketOrder 转换为 MarketOrderPO
     * @param marketOrder 实例对象
     * @return List<MarketOrderPO>
     */
    List<MarketOrderPO> domain2Po(List<MarketOrder> marketOrder);
}