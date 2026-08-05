package xyz.foolcat.eve.evehelper.infrastructure.assembler.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AssetResponse;

/**
 * ESI 资产响应 → 领域实体转换器(防腐层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiAssetsConverter {

    /**
     * AssetResponse → Assets
     */
    @Mappings({
            @Mapping(target = "ownerId", ignore = true)
    })
    Assets toDomain(AssetResponse assetResponse);
}