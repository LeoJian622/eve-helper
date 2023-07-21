package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.BlueprintsData;
import xyz.foolcat.eve.evehelper.dto.system.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.dto.system.BlueprintFormulaDTO;

import java.util.List;

@Mapper
public interface BlueprintsDataMapper extends BaseMapper<BlueprintsData> {

    int batchInsert(@Param("list") List<BlueprintsData> list);

    List<BlueprintCostDTO> calcluateCost(@Param("type_id") Integer typeId);

    List<BlueprintFormulaDTO> queryBlueprintFormulaByTypeId(@Param("type_id") Integer typeId);
}