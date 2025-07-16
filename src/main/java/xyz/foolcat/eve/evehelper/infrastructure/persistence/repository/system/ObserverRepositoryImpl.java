package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.ObserverRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.ObserverPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.ObserverMapper;
import java.util.List;

@Repository
public class ObserverRepositoryImpl implements ObserverRepository {
    @Autowired
    private ObserverMapper observerMapper;

    @Override
    public int updateBatch(List<ObserverPO> list) {
        return observerMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<ObserverPO> list) {
        return observerMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<ObserverPO> list) {
        return observerMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(ObserverPO record) {
        return observerMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(ObserverPO record) {
        return observerMapper.insertOrUpdateSelective(record);
    }
} 