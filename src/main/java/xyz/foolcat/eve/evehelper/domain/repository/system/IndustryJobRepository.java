package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;
import java.util.List;

public interface IndustryJobRepository  {
    int updateBatch(List<IndustryJobPO> list);
    int updateBatchSelective(List<IndustryJobPO> list);
    int batchInsert(List<IndustryJobPO> list);
    int insertOrUpdate(IndustryJobPO record);
    int insertOrUpdateSelective(IndustryJobPO record);
    int batchInsertOrUpdate(List<IndustryJobPO> list);
} 