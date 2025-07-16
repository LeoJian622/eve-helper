package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EsiConfig;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EsiConfigPO;

/**
 * ESI配置转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiConfigAssembler {

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
} 