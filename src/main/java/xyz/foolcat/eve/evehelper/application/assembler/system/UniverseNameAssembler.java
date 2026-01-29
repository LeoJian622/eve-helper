package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.Id2NameResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.UniverseNamePO;

import java.util.List;


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

    /**
     * Id2NameResponse 转换为 UniverseName
     * @param id2NameResponse ESI 返回的ID名称对象
     * @return UniverseName
     */
    UniverseName id2NameResponse2UniverseName(Id2NameResponse id2NameResponse);
} 