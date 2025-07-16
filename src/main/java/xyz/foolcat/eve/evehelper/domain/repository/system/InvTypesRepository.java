package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.interfaces.web.vo.InvTypesVO;

import java.util.List;

public interface InvTypesRepository {
    int updateBatch(List<InvTypes> list);

    int updateBatchSelective(List<InvTypes> list);

    int batchInsert(List<InvTypes> list);

    int insertOrUpdate(InvTypes record);

    int insertOrUpdateSelective(InvTypes record);

    InvTypesVO selcetByMarketGroupId(Long marketGroupID);
} 