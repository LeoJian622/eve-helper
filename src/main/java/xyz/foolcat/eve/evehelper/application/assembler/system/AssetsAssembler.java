package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsVO;

import java.util.List;

/**
 * @author Leojan
 * date 2024-06-24 11:49
 */

@Mapper(componentModel = "spring")
public interface AssetsAssembler {

    /**
     * Assets 转换为 AssetsVO
     * @param assets
     * @return
     */
    @Mappings({
            @Mapping(target = "owner", source = "characterName"),
            @Mapping(target = "ownerId", expression = "java(assets.getOwnerId() == null ? null : String.valueOf(assets.getOwnerId()))")
    })
    AssetsVO domain2Vo(Assets assets);

    List<AssetsVO> domain2Vo(List<Assets> assets);

}
