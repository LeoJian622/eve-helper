package xyz.foolcat.eve.evehelper.application.assembler;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.Id2NameResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.UniverseNamePO;

/**
 * @author Leojan
 * date 2022-07-07 16:49
 */
@Mapper(componentModel = "spring")
public interface UniverseNameConverter {

    /**
     * Id2NameResponse 转换为 UniverseName
     * @param id2NameResponse ESI 返回的ID名称对象
     * @return UniverseName
     */
    UniverseNamePO id2NameResponse2UniverseName(Id2NameResponse id2NameResponse);
}
