package xyz.foolcat.eve.evehelper.converter.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.system.IndustryJob;
import xyz.foolcat.eve.evehelper.esi.model.IndustryJobPlacedResponse;

/**
 * ESI 生产线信息转换方法
 *
 * @author Leojan
 * date 2024-07-02 12:01
 */

@Mapper(componentModel = "spring")
public interface IndustryJobConverter {

    @Mappings(
            @Mapping(source = "corporationId", target = "corporationId")
    )
    IndustryJob toIndustryJob(IndustryJobPlacedResponse industryJobPlacedResponse, Integer corporationId);
}
