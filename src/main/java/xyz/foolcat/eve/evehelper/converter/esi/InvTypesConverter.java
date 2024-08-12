package xyz.foolcat.eve.evehelper.converter.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;
import xyz.foolcat.eve.evehelper.esi.model.TypeInfoResponse;

/**
 * ESI 物品类型转换器
 * @author Leojan
 * date 2024-07-17 17:28
 */

@Mapper(componentModel = "spring")
public interface InvTypesConverter {

    /**
     * TypeInfoResponse 转换为 InvTypes
     * @param typeInfoResponse ESI type查询返回对象
     * @return InvTypes
     */
    @Mappings({
            @Mapping(target = "factionId",ignore = true),
            @Mapping(target = "raceId",ignore = true),
            @Mapping(target = "basePrice",ignore = true),
            @Mapping(target = "soundId",ignore = true),
            @Mapping(target = "sofFactionName",ignore = true),
            @Mapping(target = "sofMaterialSetId",ignore = true),
            @Mapping(target = "metaGroupId",ignore = true),
            @Mapping(target = "variationparentTypeId",ignore = true)
    })
    InvTypes toInvTypes(TypeInfoResponse typeInfoResponse);

    default Byte map(Boolean value) {
        if (value) {
            return 1;
        }else {
            return 0;
        }
    }
}
