package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.IndustryJobRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.IndustryJobPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.IndustryJobMapper;
import java.util.List;

@Repository
public class IndustryJobRepositoryImpl implements IndustryJobRepository {
    @Autowired
    private IndustryJobMapper industryJobMapper;

    @Override
    public int updateBatch(List<IndustryJobPO> list) {
        return industryJobMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<IndustryJobPO> list) {
        return industryJobMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<IndustryJobPO> list) {
        return industryJobMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(IndustryJobPO record) {
        return industryJobMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(IndustryJobPO record) {
        return industryJobMapper.insertOrUpdateSelective(record);
    }

    @Override
    public int batchInsertOrUpdate(List<IndustryJobPO> list) {
        return industryJobMapper.batchInsertOrUpdate(list);
    }
} 