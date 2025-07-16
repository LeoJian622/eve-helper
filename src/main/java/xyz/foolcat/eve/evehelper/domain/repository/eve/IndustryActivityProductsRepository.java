package xyz.foolcat.eve.evehelper.domain.repository.eve;

import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryActivityProducts;

public interface IndustryActivityProductsRepository  {

    int insert(IndustryActivityProducts record);

    int insertSelective(IndustryActivityProducts record);

}