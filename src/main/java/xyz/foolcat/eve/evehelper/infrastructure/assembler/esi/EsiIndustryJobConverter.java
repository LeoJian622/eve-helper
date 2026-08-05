package xyz.foolcat.eve.evehelper.infrastructure.assembler.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.IndustryJobPlacedResponse;

/**
 * ESI 工业任务响应 → 领域实体转换器(防腐层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiIndustryJobConverter {

    /**
     * IndustryJobPlacedResponse → IndustryJob
     */
    @Mappings({
            @Mapping(source = "corporationId", target = "corporationId"),
            @Mapping(target = "activity", ignore = true),
            @Mapping(target = "blueprintType", ignore = true),
            @Mapping(target = "productType", ignore = true),
            @Mapping(target = "installer", ignore = true),
            @Mapping(target = "completedCharacter", ignore = true)
    })
    IndustryJob toDomain(IndustryJobPlacedResponse industryJobPlacedResponse, Integer corporationId);
}