package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsPO;

import java.util.List;

/**
 * Blueprints 领域实体与 BlueprintsPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface BlueprintsPoConverter {

    /**
     * BlueprintsPO 转换为 Blueprints
     * @param po
     * @return
     */
    Blueprints po2Domain(BlueprintsPO po);

    /**
     * Blueprints 转换为 BlueprintsPO
     * @param blueprints
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    BlueprintsPO domain2Po(Blueprints blueprints);

    List<Blueprints> po2Domain(List<BlueprintsPO> po);

    List<BlueprintsPO> domain2Po(List<Blueprints> blueprints);
}
