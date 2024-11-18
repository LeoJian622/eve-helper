package xyz.foolcat.eve.evehelper.converter;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.system.UniverseName;
import xyz.foolcat.eve.evehelper.esi.model.Id2NameResponse;

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
    UniverseName id2NameResponse2UniverseName(Id2NameResponse id2NameResponse);
}
