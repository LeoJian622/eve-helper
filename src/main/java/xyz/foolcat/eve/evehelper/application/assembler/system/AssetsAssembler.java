package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AssetsPO;

/**
 * Assets 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface AssetsAssembler {

    /**
     * AssetsPO 转换为 Assets
     * @param assetsPO
     * @return
     */
    Assets po2Entity(AssetsPO assetsPO);

    /**
     * Assets 转换为 AssetsPO
     * @param assets
     * @return
     */
    AssetsPO entity2Po(Assets assets);
} 