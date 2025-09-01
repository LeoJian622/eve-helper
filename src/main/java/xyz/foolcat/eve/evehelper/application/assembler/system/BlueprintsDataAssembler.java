package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintsData;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsDataPO;

import java.util.List;

/**
 * 蓝图数据转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface BlueprintsDataAssembler {

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