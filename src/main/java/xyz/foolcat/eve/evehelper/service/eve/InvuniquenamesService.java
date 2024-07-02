package xyz.foolcat.eve.evehelper.service.eve;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.eve.Invuniquenames;
import xyz.foolcat.eve.evehelper.mapper.eve.InvuniquenamesMapper;

import java.util.List;
@Service
public class InvuniquenamesService extends ServiceImpl<InvuniquenamesMapper, Invuniquenames> {

    
    public int updateBatch(List<Invuniquenames> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int batchInsert(List<Invuniquenames> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int insertOrUpdate(Invuniquenames record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(Invuniquenames record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
