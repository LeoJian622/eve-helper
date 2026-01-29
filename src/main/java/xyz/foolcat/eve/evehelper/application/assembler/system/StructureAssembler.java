package xyz.foolcat.eve.evehelper.application.assembler.system;

import cn.hutool.json.JSONUtil;
import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.StructuresService;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.StructurePO;

import java.util.List;


/**
 * 建筑转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface StructureAssembler {

    /**
     * StructurePO 转换为 Structure
     * @param structurePO
     * @return
     */
    Structure po2Domain(StructurePO structurePO);

    /**
     * Structure 转换为 StructurePO
     * @param structure
     * @return
     */
    StructurePO domain2Po(Structure structure);

    /**
     * StructurePO 转换为 Structure
     * @param structurePO
     * @return
     */
    List<Structure> po2Domain(List<StructurePO> structurePO);

    /**
     * Structure 转换为 StructurePO
     * @param structure
     * @return
     */
    List<StructurePO> domain2Po(List<Structure> structure);

    /**
     * StructuresInformationResponse 转换为 Structure
     * @param structuresInformationResponse ESI返回的建筑对象
     * @return Structure
     */
    Structure toStructure(StructuresInformationResponse structuresInformationResponse);

    default String map(List<StructuresService> value){
        return JSONUtil.toJsonStr(value);
    }
} 