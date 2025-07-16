package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketOrder;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketOrderPO;

;

/**
 * MarketOrder 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface MarketOrderAssembler {

    /**
     * MarketOrderPO 转换为 MarketOrder
     * @param marketOrderPO
     * @return
     */
    MarketOrder po2Entity(MarketOrderPO marketOrderPO);

    /**
     * MarketOrder 转换为 MarketOrderPO
     * @param marketOrder
     * @return
     */
    MarketOrderPO entity2Po(MarketOrder marketOrder);
} 