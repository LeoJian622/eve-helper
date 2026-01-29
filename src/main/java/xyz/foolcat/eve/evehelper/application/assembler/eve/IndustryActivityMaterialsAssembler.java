package xyz.foolcat.eve.evehelper.application.assembler.eve;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityMaterials;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryActivityMaterialsPO;

/**
 * @author yongj
 * date 2025-07-10 16:04
 */

@Mapper(componentModel = "spring")
public interface IndustryActivityMaterialsAssembler {

    /**
     * industryActivityMaterialsPO 转换为 industryActivityMaterials
     * @param industryActivityMaterialsPO
     * @return
     */
    IndustryActivityMaterials po2Entity(IndustryActivityMaterialsPO industryActivityMaterialsPO);

    /**
     * industryActivityMaterials 转换为 industryActivityMaterialsPO
     * @param industryActivityMaterials
     * @return
     */
    IndustryActivityMaterialsPO entity2Po(IndustryActivityMaterials industryActivityMaterials);
}
