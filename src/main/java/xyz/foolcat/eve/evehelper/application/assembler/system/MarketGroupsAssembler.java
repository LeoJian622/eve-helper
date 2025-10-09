package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketGroups;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketGroupsPO;

import java.util.List;

;

/**
 * 市场组转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface MarketGroupsAssembler {

    /**
     * MarketGroupsPO 转换为 MarketGroups
     * @param marketGroupsPO
     * @return
     */
    MarketGroups po2Domain(MarketGroupsPO marketGroupsPO);

    /**
     * MarketGroups 转换为 MarketGroupsPO
     * @param marketGroups
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    MarketGroupsPO domain2Po(MarketGroups marketGroups);

    /**
     * MarketGroupsPO 转换为 MarketGroups
     * @param marketGroupsPO
     * @return
     */
    List<MarketGroups> po2Domain(List<MarketGroupsPO> marketGroupsPO);

    /**
     * MarketGroups 转换为 MarketGroupsPO
     * @param marketGroups
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    List<MarketGroupsPO> domain2Po(List<MarketGroups> marketGroups);
} 