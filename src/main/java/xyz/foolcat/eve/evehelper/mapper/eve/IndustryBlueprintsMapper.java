package xyz.foolcat.eve.evehelper.mapper.eve;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.eve.IndustryBlueprints;

@Mapper
public interface IndustryBlueprintsMapper {
    int deleteByPrimaryKey(Integer blueprinttypeid);

    int insert(IndustryBlueprints record);

    int insertSelective(IndustryBlueprints record);

    IndustryBlueprints selectByPrimaryKey(Integer blueprinttypeid);

    int updateByPrimaryKeySelective(IndustryBlueprints record);

    int updateByPrimaryKey(IndustryBlueprints record);

    int updateBatch(List<IndustryBlueprints> list);
}