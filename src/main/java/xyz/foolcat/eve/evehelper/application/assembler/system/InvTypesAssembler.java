package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.InvTypesPO;

/**
 * 物品转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface InvTypesAssembler {

    /**
     * InvTypesPO 转换为 InvTypes
     * @param invTypesPO
     * @return
     */
    InvTypes po2Domain(InvTypesPO invTypesPO);

    /**
     * InvTypes 转换为 InvTypesPO
     * @param invTypes
     * @return
     */
    InvTypesPO domain2Po(InvTypes invTypes);
} 