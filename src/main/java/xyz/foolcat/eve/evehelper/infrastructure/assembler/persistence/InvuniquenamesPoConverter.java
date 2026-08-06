package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.InvUniqueNames;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.InvUniqueNamesPO;

/**
 * InvUniqueNames 领域实体与 InvUniqueNamesPO 持久化对象转换器(基础设施层)。
 *
 * @author yongj
 */
@Mapper(componentModel = "spring")
public interface InvuniquenamesPoConverter {

    /**
     * InvUniqueNamesPO 转换为 InvUniqueNames
     * @param invUniqueNamesPO
     * @return
     */
    InvUniqueNames po2Domain(InvUniqueNamesPO invUniqueNamesPO);

    /**
     * InvUniqueNames 转换为 InvUniqueNamesPO
     * @param invUniqueNames
     * @return
     */
    InvUniqueNamesPO domain2Po(InvUniqueNames invUniqueNames);
}
