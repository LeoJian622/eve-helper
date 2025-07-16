package xyz.foolcat.eve.evehelper.domain.repository.eve;

import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityMaterials;

/**
 * @author yongj
 */
public interface IndustryActivityMaterialsRepository{

    int insert(IndustryActivityMaterials record);

    int insertSelective(IndustryActivityMaterials record);

}