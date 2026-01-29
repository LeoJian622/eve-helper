package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AssetResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AssetsPO;

import java.util.List;

/**
 * @author Leojan
 * date 2024-06-24 11:49
 */

@Mapper(componentModel = "spring")
public interface AssetsAssembler {

    /**
     * StructuresInformationResponse 转换为 Structure
     *
     * @param assetResponse ESI返回的资产对象
     * @return Structure
     */
    @Mappings({
            @Mapping(target = "ownerId", ignore = true)
    })
    Assets toAssets(AssetResponse assetResponse);

    /**
     * Assets 转换为 AssetsPO
     * @param assets
     * @return
     */
    AssetsPO domain2Po(Assets assets);

    /**
     * AssetsPO 转换为 Assets
     * @param assetsPO
     * @return
     */
    Assets po2Domain(AssetsPO assetsPO);

    List<AssetsPO> domain2Po(List<Assets> assets);

    List<Assets> po2Domain(List<AssetsPO> assetsPO);

}
