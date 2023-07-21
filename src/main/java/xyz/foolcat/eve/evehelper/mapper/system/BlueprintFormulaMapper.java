package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.BlueprintFormula;

@Mapper
public interface BlueprintFormulaMapper extends BaseMapper<BlueprintFormula> {
    int batchInsert(@Param("list") List<BlueprintFormula> list);
}