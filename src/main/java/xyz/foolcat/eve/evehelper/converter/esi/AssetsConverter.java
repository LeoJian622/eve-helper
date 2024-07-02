package xyz.foolcat.eve.evehelper.converter.esi;

import org.mapstruct.Mapper;
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
    Assets assetResponse2Assets(AssetResponse assetResponse);

}
