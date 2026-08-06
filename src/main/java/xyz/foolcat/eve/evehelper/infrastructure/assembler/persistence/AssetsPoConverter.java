package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AssetsPO;

import java.util.List;

/**
 * Assets 领域实体与 AssetsPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface AssetsPoConverter {

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
