package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryBlueprints;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryBlueprintsPO;

/**
 * IndustryBlueprints 领域实体与 IndustryBlueprintsPO 持久化对象转换器(基础设施层)。
 *
 * @author yongj
 */
@Mapper(componentModel = "spring")
public interface IndustryBlueprintsPoConverter {

    /**
     * IndustryBlueprintsPO 转换为 IndustryBlueprints
     * @param industryBlueprintsPO
     * @return
     */
    IndustryBlueprints po2Domain(IndustryBlueprintsPO industryBlueprintsPO);

    /**
     * IndustryBlueprints 转换为 IndustryBlueprintsPO
     * @param industryBlueprints
     * @return
     */
    IndustryBlueprintsPO domain2Po(IndustryBlueprints industryBlueprints);
}
