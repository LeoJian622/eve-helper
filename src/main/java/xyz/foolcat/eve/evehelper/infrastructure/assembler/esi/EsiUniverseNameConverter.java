package xyz.foolcat.eve.evehelper.infrastructure.assembler.esi;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.Id2NameResponse;

/**
 * ESI 名称解析响应 → 领域实体转换器(防腐层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiUniverseNameConverter {

    /**
     * Id2NameResponse → UniverseName
     */
    UniverseName toDomain(Id2NameResponse id2NameResponse);
}