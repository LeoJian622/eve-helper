package xyz.foolcat.eve.evehelper.domain.repository.eve;

import xyz.foolcat.eve.evehelper.domain.model.entity.eve.IndustryBlueprints;

import java.util.List;

public interface IndustryBlueprintsRepository
{
    int deleteByPrimaryKey(Integer blueprinttypeid);

    int insert(IndustryBlueprints record);

    int insertSelective(IndustryBlueprints record);

    IndustryBlueprints selectByPrimaryKey(Integer blueprinttypeid);

    int updateByPrimaryKeySelective(IndustryBlueprints record);

    int updateByPrimaryKey(IndustryBlueprints record);

    int updateBatch(List<IndustryBlueprints> list);

}