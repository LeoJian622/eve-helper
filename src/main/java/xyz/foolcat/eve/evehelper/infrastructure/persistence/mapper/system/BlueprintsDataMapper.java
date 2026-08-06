package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintCostDTO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsDataPO;

import java.util.List;

@Mapper
public interface BlueprintsDataMapper extends BaseMapper<BlueprintsDataPO> {
    int batchInsert(List<BlueprintsDataPO> blueprintsDataPOS);

    int insertOrUpdateSelective(BlueprintsDataPO blueprintsDataPO);

    List<BlueprintCostDTO> calcluateCost(Integer typeId);
    // 只保留基础 CRUD
}
