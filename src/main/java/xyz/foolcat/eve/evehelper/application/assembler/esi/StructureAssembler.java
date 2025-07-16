package xyz.foolcat.eve.evehelper.application.assembler.esi;

import cn.hutool.json.JSONUtil;
import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.StructuresService;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.StructurePO;

import java.util.List;

/**
 * @author Leojan
 * date 2024-06-24 11:49
 */

@Mapper(componentModel = "spring")
public interface StructureAssembler {

    /**
     * StructuresInformationResponse 转换为 Structure
     * @param structuresInformationResponse ESI返回的建筑对象
     * @return Structure
     */
    StructurePO toStructure(StructuresInformationResponse structuresInformationResponse);

    default String map(List<StructuresService> value){
        return JSONUtil.toJsonStr(value);
    }
}
