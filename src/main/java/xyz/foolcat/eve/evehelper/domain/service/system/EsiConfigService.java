package xyz.foolcat.eve.evehelper.domain.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EsiConfig;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EsiConfigMapper;

import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
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

    public int insertOrUpdate(EsiConfig record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(EsiConfig record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

