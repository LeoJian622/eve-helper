package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.TypeInfoResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.InvTypesPO;

import java.util.List;

/**
 * 物品转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface InvTypesAssembler {

    /**
     * InvTypesPO 转换为 InvTypes
     * @param invTypesPO
     * @return
     */
    InvTypes po2Domain(InvTypesPO invTypesPO);

    /**
     * InvTypes 转换为 InvTypesPO
     * @param invTypes
     * @return
     */
    InvTypesPO domain2Po(InvTypes invTypes);

    /**
     * InvTypesPO 转换为 InvTypes
     * @param invTypesPO
     * @return
     */
    List<InvTypes> po2Domain(List<InvTypesPO> invTypesPO);

    /**
     * InvTypes 转换为 InvTypesPO
     * @param invTypes
     * @return
     */
    List<InvTypesPO> domain2Po(List<InvTypes> invTypes);

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