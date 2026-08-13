package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;

import java.util.List;

/**
 * @author Leojan
 */
public interface InvTypesRepository {
    int updateBatch(List<InvTypes> list);

    int updateBatchSelective(List<InvTypes> list);

    int batchInsert(List<InvTypes> list);

    public boolean insertOrUpdate(InvTypes record);

    int insertOrUpdateSelective(InvTypes record);

    List<InvTypes> selectTypeNameByIds(List<Integer> typeIds);

    InvTypes selectOneByName(String name);

    InvTypes selectOneById(int id);
}