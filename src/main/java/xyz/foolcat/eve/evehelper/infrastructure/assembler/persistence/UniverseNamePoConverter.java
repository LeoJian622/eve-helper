package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.UniverseNamePO;

import java.util.List;

/**
 * UniverseName 领域实体与 UniverseNamePO 持久化对象转换器(基础设施层)。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface UniverseNamePoConverter {

    /**
     * UniverseNamePO 转换为 UniverseName
     * @param universeNamePO
     * @return
     */
    UniverseName po2Domain(UniverseNamePO universeNamePO);

    /**
     * UniverseName 转换为 UniverseNamePO
     * @param universeName
     * @return
     */
    UniverseNamePO domain2Po(UniverseName universeName);

    /**
     * UniverseNamePO 列表转换为 UniverseName 列表
     * @param universeNamePOs
     * @return
     */
    List<UniverseName> po2Domain(List<UniverseNamePO> universeNamePOs);

    /**
     * UniverseName 列表转换为 UniverseNamePO 列表
     * @param universeNames
     * @return
     */
    List<UniverseNamePO> domain2Po(List<UniverseName> universeNames);
}
