package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.StructurePO;

;

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
} 