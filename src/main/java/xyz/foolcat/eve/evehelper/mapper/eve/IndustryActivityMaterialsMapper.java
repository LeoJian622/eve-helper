package xyz.foolcat.eve.evehelper.mapper.eve;

import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.eve.IndustryActivityMaterials;

@Mapper
public interface IndustryActivityMaterialsMapper {
    int insert(IndustryActivityMaterials record);

    int insertSelective(IndustryActivityMaterials record);
}