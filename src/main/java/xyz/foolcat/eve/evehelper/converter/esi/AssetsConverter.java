package xyz.foolcat.eve.evehelper.converter.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.esi.model.AssetResponse;

/**
 * @author Leojan
 * date 2024-06-24 11:49
 */

@Mapper(componentModel = "spring")
public interface AssetsConverter {

    /**
     * StructuresInformationResponse 转换为 Structure
     * @param assetResponse ESI返回的资产对象
     * @return Structure
     */
    @Mappings({
            @Mapping(target = "ownerId",ignore = true)
    })
    Assets toAssets(AssetResponse assetResponse);

}
