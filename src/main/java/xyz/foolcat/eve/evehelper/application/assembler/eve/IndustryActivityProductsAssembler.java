package xyz.foolcat.eve.evehelper.application.assembler.eve;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityProducts;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryActivityProductsPO;

/**
 * @author yongj
 * date 2025-07-10 16:15
 */

@Mapper(componentModel = "spring")
public interface IndustryActivityProductsAssembler {
    /**
     * IndustryActivityProductsPO 转换为 IndustryActivityProducts
     * @param IndustryActivityProductsPO
     * @return
     */
    IndustryActivityProducts po2Entity(IndustryActivityProductsPO IndustryActivityProductsPO);

    /**
     * IndustryActivityProducts 转换为 IndustryActivityProductsPO
     *
     * @param IndustryActivityProducts
     * @return
     */
    IndustryActivityProductsPO entity2Po(IndustryActivityProducts IndustryActivityProducts);
}
