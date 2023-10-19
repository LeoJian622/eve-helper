package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.system.BlueprintFormula;
import xyz.foolcat.eve.evehelper.mapper.system.BlueprintFormulaMapper;

import java.util.List;

@Service
public class BlueprintFormulaService extends ServiceImpl<BlueprintFormulaMapper, BlueprintFormula> {


    public int batchInsert(List<BlueprintFormula> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(BlueprintFormula record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(BlueprintFormula record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

