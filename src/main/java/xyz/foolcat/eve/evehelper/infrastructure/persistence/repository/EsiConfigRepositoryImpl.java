package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.EsiConfigRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EsiConfigPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EsiConfigMapper;
import java.util.List;

@Repository
public class EsiConfigRepositoryImpl implements EsiConfigRepository {
    @Autowired
    private EsiConfigMapper esiConfigMapper;

    @Override
    public int updateBatch(List<EsiConfigPO> list) {
        return esiConfigMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<EsiConfigPO> list) {
        return esiConfigMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<EsiConfigPO> list) {
        return esiConfigMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(EsiConfigPO record) {
        return esiConfigMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(EsiConfigPO record) {
        return esiConfigMapper.insertOrUpdateSelective(record);
    }
} 