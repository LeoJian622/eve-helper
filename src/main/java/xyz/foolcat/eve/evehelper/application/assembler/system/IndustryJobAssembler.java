package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.IndustryJobPlacedResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;

import java.util.List;

/**
 * IndustryJob 实体转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface IndustryJobAssembler {

    /**
     * IndustryJobPO 转换为 IndustryJob
     * @param industryJobPO
     * @return
     */
    IndustryJob po2Entity(IndustryJobPO industryJobPO);

    /**
     * IndustryJob 转换为 IndustryJobPO
     * @param industryJob
     * @return
     */
    IndustryJobPO entity2Po(IndustryJob industryJob);

    /**
     * IndustryJobPO 转换为 IndustryJob
     * @param industryJobPO
     * @return
     */
    List<IndustryJob> po2Entity(List<IndustryJobPO> industryJobPO);

    /**
     * IndustryJob 转换为 IndustryJobPO
     * @param industryJob
     * @return
     */
    List<IndustryJobPO> entity2Po(List<IndustryJob> industryJob);

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
    IndustryJobPO toIndustryJobPo(IndustryJobPlacedResponse industryJobPlacedResponse, Integer corporationId);

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
    IndustryJob toIndustryJob(IndustryJobPlacedResponse industryJobPlacedResponse, Integer corporationId);
} 