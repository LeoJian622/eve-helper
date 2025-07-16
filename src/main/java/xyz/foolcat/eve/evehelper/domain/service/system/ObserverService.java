package xyz.foolcat.eve.evehelper.domain.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Observer;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.ObserverMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class ObserverService extends ServiceImpl<ObserverMapper, Observer> {

    public int updateBatch(List<Observer> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<Observer> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<Observer> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(Observer record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Observer record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}

