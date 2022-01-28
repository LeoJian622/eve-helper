package xyz.foolcat.eve.evehelper.mapper.eve;

import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.eve.IndustryActivityProducts;

@Mapper
public interface IndustryActivityProductsMapper {
    int insert(IndustryActivityProducts record);

    int insertSelective(IndustryActivityProducts record);
}