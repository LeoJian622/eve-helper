package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintsData;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsDataPO;

import java.util.List;

/**
 * BlueprintsData 领域实体与 BlueprintsDataPO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface BlueprintsDataPoConverter {

    /**
     * BlueprintsDataPO 转换为 BlueprintsData
     * @param blueprintsDataPO
     * @return
     */
    BlueprintsData po2Domain(BlueprintsDataPO blueprintsDataPO);

    /**
     * BlueprintsData 转换为 BlueprintsDataPO
     * @param blueprintsData
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    BlueprintsDataPO domain2Po(BlueprintsData blueprintsData);

    List<BlueprintsData> po2Domain(List<BlueprintsDataPO> blueprintsDataPO);

    List<BlueprintsDataPO> domain2Po(List<BlueprintsData> blueprintsData);
}
