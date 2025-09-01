package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.application.dto.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsPO;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.BlueprintsVO;

import java.util.List;

/**
 * 蓝图组装器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface BlueprintsAssembler {

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

    List<BlueprintsVO> dto2Vo(List<BlueprintsDTO> dtoList);
} 