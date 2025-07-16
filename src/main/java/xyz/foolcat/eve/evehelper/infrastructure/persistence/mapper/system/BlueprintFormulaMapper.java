package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintFormulaPO;

@Mapper
public interface BlueprintFormulaMapper extends BaseMapper<BlueprintFormulaPO> {
    // 只保留基础 CRUD
}
