package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.mapper.system.EveAccountMapper;

import java.util.List;
@Service
public class EveAccountService extends ServiceImpl<EveAccountMapper, EveAccount> {

    
    public int updateBatch(List<EveAccount> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int batchInsert(List<EveAccount> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int insertOrUpdate(EveAccount record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(EveAccount record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}
