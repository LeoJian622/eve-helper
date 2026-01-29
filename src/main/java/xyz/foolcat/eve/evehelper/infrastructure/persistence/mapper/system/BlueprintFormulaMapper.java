package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintFormulaPO;

import java.util.List;

@Mapper
public interface BlueprintFormulaMapper extends BaseMapper<BlueprintFormulaPO> {

    int batchInsert(List<BlueprintFormulaPO> blueprintFormulaPOS);

    int insertOrUpdate(BlueprintFormulaPO blueprintFormulaPO);

    int insertOrUpdateSelective(BlueprintFormulaPO blueprintFormulaPO);
    // 只保留基础 CRUD
}
