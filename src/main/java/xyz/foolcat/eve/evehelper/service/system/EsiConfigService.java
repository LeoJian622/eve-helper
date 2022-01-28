package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xyz.foolcat.eve.evehelper.domain.system.EsiConfig;
import java.util.List;
import xyz.foolcat.eve.evehelper.mapper.system.EsiConfigMapper;
@Service
public class EsiConfigService extends ServiceImpl<EsiConfigMapper, EsiConfig> {

    
    public int updateBatch(List<EsiConfig> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchSelective(List<EsiConfig> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<EsiConfig> list) {
        return baseMapper.batchInsert(list);
    }
}
