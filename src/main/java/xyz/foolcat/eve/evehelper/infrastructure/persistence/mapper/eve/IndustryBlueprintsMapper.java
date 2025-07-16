package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.eve;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.eve.IndustryBlueprintsPO;

import java.util.List;

@Mapper
public interface IndustryBlueprintsMapper extends BaseMapper<IndustryBlueprintsPO> {

    int deleteByPrimaryKey(Integer blueprinttypeid);

    int insertSelective(IndustryBlueprintsPO record);

    IndustryBlueprintsPO selectByPrimaryKey(Integer blueprinttypeid);

    int updateByPrimaryKeySelective(IndustryBlueprintsPO industryBlueprintsPO);

    int updateByPrimaryKey(IndustryBlueprintsPO industryBlueprintsPO);

    int updateBatch(List<IndustryBlueprintsPO> industryBlueprintsPOS);
}
