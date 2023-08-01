package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.system.EveCharacter;
import xyz.foolcat.eve.evehelper.mapper.system.EveCharacterMapper;

import java.util.List;
@Service
public class EveCharacterService extends ServiceImpl<EveCharacterMapper, EveCharacter> {

    
    public int updateBatch(List<EveCharacter> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int batchInsert(List<EveCharacter> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int insertOrUpdate(EveCharacter record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(EveCharacter record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
