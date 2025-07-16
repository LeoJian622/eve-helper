package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;

import java.util.List;

public interface UniverseNameRepository {
    int updateBatch(List<UniverseName> list);

    int updateBatchSelective(List<UniverseName> list);

    int batchInsert(List<UniverseName> list);

    int insertOrUpdate(UniverseName record);

    int insertOrUpdateSelective(UniverseName record);
} 