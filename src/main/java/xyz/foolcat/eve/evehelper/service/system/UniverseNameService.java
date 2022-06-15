package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xyz.foolcat.eve.evehelper.mapper.system.UniverseNameMapper;
import java.util.List;
import xyz.foolcat.eve.evehelper.domain.system.UniverseName;
@Service
public class UniverseNameService extends ServiceImpl<UniverseNameMapper, UniverseName> {

    
    public int updateBatch(List<UniverseName> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchSelective(List<UniverseName> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<UniverseName> list) {
        return baseMapper.batchInsert(list);
    }
}
