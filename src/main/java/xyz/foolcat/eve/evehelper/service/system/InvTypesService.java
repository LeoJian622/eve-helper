package xyz.foolcat.eve.evehelper.service.system;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;
import xyz.foolcat.eve.evehelper.mapper.system.InvTypesMapper;
@Service
public class InvTypesService extends ServiceImpl<InvTypesMapper, InvTypes> {

    
    public int updateBatch(List<InvTypes> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchSelective(List<InvTypes> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<InvTypes> list) {
        return baseMapper.batchInsert(list);
    }
}
