package xyz.foolcat.eve.evehelper.converter.esi;

import cn.hutool.json.JSONUtil;
import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.system.Structure;
import xyz.foolcat.eve.evehelper.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.esi.model.sub.StructuresService;

import java.util.List;

/**
 * @author Leojan
 * @date 2024-06-24 11:49
 */

@Mapper(componentModel = "spring")
public interface StructureConverter {

    /**
     * StructuresInformationResponse 转换为 Structure
     * @param structuresInformationResponse
     * @return Structure
     */
    Structure structuresInformationResponse2Structure(StructuresInformationResponse structuresInformationResponse);

    default String map(List<StructuresService> value){
        return JSONUtil.toJsonStr(value);
    }
}
