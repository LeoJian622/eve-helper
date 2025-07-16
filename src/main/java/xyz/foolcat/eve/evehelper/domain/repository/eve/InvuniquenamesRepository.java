package xyz.foolcat.eve.evehelper.domain.repository.eve;

import xyz.foolcat.eve.evehelper.domain.model.entity.eve.Invuniquenames;

import java.util.List;

public interface InvuniquenamesRepository{

    int updateBatch(List<Invuniquenames> list);

    int batchInsert(List<Invuniquenames> list);

    int insertOrUpdate(Invuniquenames record);

    int insertOrUpdateSelective(Invuniquenames record);
} 