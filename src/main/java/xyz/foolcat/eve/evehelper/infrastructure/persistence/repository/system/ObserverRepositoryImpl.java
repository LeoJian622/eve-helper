package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Observer;
import xyz.foolcat.eve.evehelper.domain.repository.system.ObserverRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.ObserverPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.ObserverMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class ObserverRepositoryImpl implements ObserverRepository {

    private final ObserverMapper observerMapper;
    private final ObserverPoConverter observerPoConverter;

    @Override
    public int updateBatch(List<Observer> list) {
        return observerMapper.updateBatch(observerPoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<Observer> list) {
        return observerMapper.updateBatchSelective(observerPoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<Observer> list) {
        return observerMapper.batchInsert(observerPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(Observer record) {
        return observerMapper.insertOrUpdate(observerPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Observer record) {
        return observerMapper.insertOrUpdateSelective(observerPoConverter.domain2Po(record));
    }
}
