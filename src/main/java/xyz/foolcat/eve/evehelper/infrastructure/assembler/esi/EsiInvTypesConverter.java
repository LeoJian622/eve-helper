package xyz.foolcat.eve.evehelper.infrastructure.assembler.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.TypeInfoResponse;

/**
 * ESI 物品类型响应 → 领域实体转换器(防腐层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiInvTypesConverter {

    /**
     * TypeInfoResponse → InvTypes
     */
    @Mappings({
            @Mapping(target = "factionId", ignore = true),
            @Mapping(target = "raceId", ignore = true),
            @Mapping(target = "basePrice", ignore = true),
            @Mapping(target = "soundId", ignore = true),
            @Mapping(target = "sofFactionName", ignore = true),
            @Mapping(target = "sofMaterialSetId", ignore = true),
            @Mapping(target = "metaGroupId", ignore = true),
            @Mapping(target = "variationparentTypeId", ignore = true)
    })
    InvTypes toDomain(TypeInfoResponse typeInfoResponse);
}