package xyz.foolcat.eve.evehelper.infrastructure.assembler.esi;

import cn.hutool.json.JSONUtil;
import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.StructuresService;

import java.util.List;

/**
 * ESI 建筑响应 → 领域实体转换器(防腐层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiStructureConverter {

    /**
     * StructuresInformationResponse → Structure
     */
    Structure toDomain(StructuresInformationResponse structuresInformationResponse);

    /**
     * 服务列表序列化为 JSON 字符串。
     */
    default String map(List<StructuresService> value) {
        return JSONUtil.toJsonStr(value);
    }
}