package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Observer;
import xyz.foolcat.eve.evehelper.domain.repository.system.ObserverRepository;

import java.util.List;

/**
 * @author Leojan
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class ObserverService {

    private final ObserverRepository observerRepository;

    public int updateBatch(List<Observer> list) {
        return observerRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<Observer> list) {
        return observerRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<Observer> list) {
        return observerRepository.batchInsert(list);
    }

    public int insertOrUpdate(Observer record) {
        return observerRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Observer record) {
        return observerRepository.insertOrUpdateSelective(record);
    }
}

