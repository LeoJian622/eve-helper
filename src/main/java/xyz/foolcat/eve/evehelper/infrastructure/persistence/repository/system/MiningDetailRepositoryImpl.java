package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.MiningDetailRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.MiningDetailPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.MiningDetailMapper;

import java.util.List;

@Repository
public class MiningDetailRepositoryImpl implements MiningDetailRepository {
    @Autowired
    private MiningDetailMapper miningDetailMapper;

    @Override
    public int updateBatch(List<MiningDetailPO> list) {
        return miningDetailMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<MiningDetailPO> list) {
        return miningDetailMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<MiningDetailPO> list) {
        return miningDetailMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(MiningDetailPO record) {
        return miningDetailMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(MiningDetailPO record) {
        return miningDetailMapper.insertOrUpdateSelective(record);
    }
    // TODO: 实现 BaseRepository<MiningDetail, Long> 的方法
} 