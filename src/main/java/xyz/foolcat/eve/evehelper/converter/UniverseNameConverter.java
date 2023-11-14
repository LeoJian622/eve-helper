package xyz.foolcat.eve.evehelper.converter;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.system.UniverseName;
import xyz.foolcat.eve.evehelper.dto.esi.UniverseNameResponeDTO;

/**
 * @author Leojan
 * date 2022-07-07 16:49
 */
@Mapper(componentModel = "spring")
public interface UniverseNameConverter {

    /**
     * universeNameResponeDTO 转换为 UniverseName
     * @param universeNameResponeDTO
     * @return
     */
    UniverseName universeNameResponeDTO2UniverseName(UniverseNameResponeDTO universeNameResponeDTO);
}
