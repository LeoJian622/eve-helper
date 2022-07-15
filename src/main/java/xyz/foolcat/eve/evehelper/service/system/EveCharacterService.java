package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.mapper.system.EveCharacterMapper;
import xyz.foolcat.eve.evehelper.domain.system.EveCharacter;
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class EveCharacterService extends ServiceImpl<EveCharacterMapper, EveCharacter> {

    public int updateBatch(List<EveCharacter> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchSelective(List<EveCharacter> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<EveCharacter> list) {
        return baseMapper.batchInsert(list);
    }
}
