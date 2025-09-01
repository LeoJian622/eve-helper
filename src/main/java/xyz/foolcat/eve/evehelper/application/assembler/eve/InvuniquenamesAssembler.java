package xyz.foolcat.eve.evehelper.application.assembler.eve;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.InvUniqueNames;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.InvUniqueNamesPO;

/**
 * @author yongj
 * date 2025-07-10 16:15
 */
@Mapper(componentModel = "spring")
public interface InvuniquenamesAssembler {
    /**
     * InvuniquenamesPO 转换为 Invuniquenames
     * @param InvuniquenamesPO
     * @return
     */
    InvUniqueNames po2Entity(InvUniqueNamesPO InvuniquenamesPO);

    /**
     * Invuniquenames 转换为 InvuniquenamesPO
     * @param Invuniquenames
     * @return
     */
    InvUniqueNamesPO entity2Po(InvUniqueNames Invuniquenames);
}
