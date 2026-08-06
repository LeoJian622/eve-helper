package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MarketGroups;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MarketGroupsPO;

import java.util.List;

/**
 * MarketGroups 领域实体与 MarketGroupsPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface MarketGroupsPoConverter {

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
