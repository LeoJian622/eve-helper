package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.UniverseNamePO;

;

/**
 * 宇宙名称转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface UniverseNameAssembler {

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
} 