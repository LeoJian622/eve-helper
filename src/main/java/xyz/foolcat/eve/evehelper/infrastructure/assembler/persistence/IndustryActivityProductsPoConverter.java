package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityProducts;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryActivityProductsPO;

/**
 * IndustryActivityProducts 领域实体与 IndustryActivityProductsPO 持久化对象转换器(基础设施层)。
 *
 * @author yongj
 */
@Mapper(componentModel = "spring")
public interface IndustryActivityProductsPoConverter {

    /**
     * IndustryActivityProductsPO 转换为 IndustryActivityProducts
     * @param industryActivityProductsPO
     * @return
     */
    IndustryActivityProducts po2Domain(IndustryActivityProductsPO industryActivityProductsPO);

    /**
     * IndustryActivityProducts 转换为 IndustryActivityProductsPO
     * @param industryActivityProducts
     * @return
     */
    IndustryActivityProductsPO domain2Po(IndustryActivityProducts industryActivityProducts);
}
