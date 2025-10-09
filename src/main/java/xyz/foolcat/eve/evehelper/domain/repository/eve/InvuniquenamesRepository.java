package xyz.foolcat.eve.evehelper.domain.repository.eve;

import xyz.foolcat.eve.evehelper.domain.model.entity.eve.InvUniqueNames;

import java.util.List;

public interface InvuniquenamesRepository{

    int updateBatch(List<InvUniqueNames> list);

    int batchInsert(List<InvUniqueNames> list);

    int insertOrUpdate(InvUniqueNames record);

    int insertOrUpdateSelective(InvUniqueNames record);

    InvUniqueNames selectById(Integer id);
}