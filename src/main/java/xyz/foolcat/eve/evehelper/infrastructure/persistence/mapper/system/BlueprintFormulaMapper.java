package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintFormulaPO;

import java.util.List;

@Mapper
public interface BlueprintFormulaMapper extends BaseMapper<BlueprintFormulaPO> {

    int batchInsert(List<BlueprintFormulaPO> blueprintFormulaPOS);

    int insertOrUpdateSelective(BlueprintFormulaPO blueprintFormulaPO);
}
