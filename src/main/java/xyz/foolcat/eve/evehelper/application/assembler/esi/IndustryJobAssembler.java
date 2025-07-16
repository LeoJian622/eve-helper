package xyz.foolcat.eve.evehelper.application.assembler.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.IndustryJobPlacedResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;

/**
 * ESI 生产线信息转换方法
 *
 * @author Leojan
 * date 2024-07-02 12:01
 */

@Mapper(componentModel = "spring")
public interface IndustryJobAssembler {

    /**
     * IndustryJobPlacedResponse 转换为 IndustryJob
     *
     * @param industryJobPlacedResponse ESI返回生产线对象
     * @param corporationId             军团ID
     * @return IndustryJob
     */
    @Mappings({
            @Mapping(source = "corporationId", target = "corporationId"),
            @Mapping(target = "activity", ignore = true),
            @Mapping(target = "blueprintType", ignore = true),
            @Mapping(target = "productType", ignore = true),
            @Mapping(target = "installer", ignore = true),
            @Mapping(target = "completedCharacter", ignore = true)
    })
    IndustryJobPO toIndustryJob(IndustryJobPlacedResponse industryJobPlacedResponse, Integer corporationId);
}
