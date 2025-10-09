package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.ObserverAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Observer;
import xyz.foolcat.eve.evehelper.domain.repository.system.ObserverRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.ObserverMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ObserverRepositoryImpl implements ObserverRepository {

    private final ObserverMapper observerMapper;
    private final ObserverAssembler observerAssembler;

    @Override
    public int updateBatch(List<Observer> list) {
        return observerMapper.updateBatch(observerAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<Observer> list) {
        return observerMapper.updateBatchSelective(observerAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<Observer> list) {
        return observerMapper.batchInsert(observerAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(Observer record) {
        return observerMapper.insertOrUpdate(observerAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Observer record) {
        return observerMapper.insertOrUpdateSelective(observerAssembler.domain2Po(record));
    }
} 