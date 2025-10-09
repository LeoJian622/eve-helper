package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;

import java.util.List;

public interface IndustryJobRepository  {

    int updateBatch(List<IndustryJob> list);

    int updateBatchSelective(List<IndustryJob> list);

    int batchInsert(List<IndustryJob> list);

    int insertOrUpdate(IndustryJob record);

    int insertOrUpdateSelective(IndustryJob record);

    int batchInsertOrUpdate(List<IndustryJob> list);

    List<IndustryJob> selectByCorpIdAndStatus(Integer corpId, String statusDelivered);
}