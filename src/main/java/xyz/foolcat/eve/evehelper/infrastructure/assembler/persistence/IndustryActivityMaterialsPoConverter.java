package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityMaterials;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryActivityMaterialsPO;

/**
 * IndustryActivityMaterials 领域实体与 IndustryActivityMaterialsPO 持久化对象转换器(基础设施层)。
 *
 * @author yongj
 */
@Mapper(componentModel = "spring")
public interface IndustryActivityMaterialsPoConverter {

    /**
     * IndustryActivityMaterialsPO 转换为 IndustryActivityMaterials
     * @param industryActivityMaterialsPO
     * @return
     */
    IndustryActivityMaterials po2Domain(IndustryActivityMaterialsPO industryActivityMaterialsPO);

    /**
     * IndustryActivityMaterials 转换为 IndustryActivityMaterialsPO
     * @param industryActivityMaterials
     * @return
     */
    IndustryActivityMaterialsPO domain2Po(IndustryActivityMaterials industryActivityMaterials);
}
