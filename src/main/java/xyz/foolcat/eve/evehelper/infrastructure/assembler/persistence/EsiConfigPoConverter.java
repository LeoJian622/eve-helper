package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EsiConfig;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EsiConfigPO;

import java.util.List;

/**
 * EsiConfig 领域实体与 EsiConfigPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiConfigPoConverter {

    /**
     * EsiConfigPO 转换为 EsiConfig
     * @param esiConfigPO
     * @return
     */
    EsiConfig po2Domain(EsiConfigPO esiConfigPO);

    /**
     * EsiConfig 转换为 EsiConfigPO
     * @param esiConfig
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    EsiConfigPO domain2Po(EsiConfig esiConfig);

    List<EsiConfig> po2Domain(List<EsiConfigPO> esiConfigPO);

    List<EsiConfigPO> domain2Po(List<EsiConfig> esiConfig);
}
