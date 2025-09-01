package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.application.dto.response.BlueprintFormulaDTO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsDataPO;

import java.util.List;

@Mapper
public interface BlueprintsDataMapper extends BaseMapper<BlueprintsDataPO> {
    int batchInsert(List<BlueprintsDataPO> blueprintsDataPOS);

    int insertOrUpdate(BlueprintsDataPO blueprintsDataPO);

    int insertOrUpdateSelective(BlueprintsDataPO blueprintsDataPO);

    List<BlueprintCostDTO> calcluateCost(Integer typeId);

    List<BlueprintFormulaDTO> queryBlueprintFormulaByTypeId(@Param("type_id") Integer typeId);
    // 只保留基础 CRUD
}
