package xyz.foolcat.eve.evehelper.application.assembler.eve;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryBlueprints;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryBlueprintsPO;

/**
 * @author yongj
 * date 2025-07-10 16:15
 */

@Mapper(componentModel = "spring")
public interface IndustryBlueprintsAssembler {
    /**
     * IndustryBlueprintsPO 转换为 IndustryBlueprints
     *
     * @param IndustryBlueprintsPO
     * @return
     */
    IndustryBlueprints po2Entity(IndustryBlueprintsPO IndustryBlueprintsPO);

    /**
     * IndustryBlueprints 转换为 IndustryBlueprintsPO
     *
     * @param IndustryBlueprints
     * @return
     */
    IndustryBlueprintsPO entity2Po(IndustryBlueprints IndustryBlueprints);
}
