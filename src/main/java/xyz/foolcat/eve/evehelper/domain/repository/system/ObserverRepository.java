package xyz.foolcat.eve.evehelper.domain.repository.system;


import xyz.foolcat.eve.evehelper.domain.model.entity.system.Observer;

import java.util.List;

public interface ObserverRepository {
    int updateBatch(List<Observer> list);

    int updateBatchSelective(List<Observer> list);

    int batchInsert(List<Observer> list);

    int insertOrUpdate(Observer record);

    int insertOrUpdateSelective(Observer record);
} 