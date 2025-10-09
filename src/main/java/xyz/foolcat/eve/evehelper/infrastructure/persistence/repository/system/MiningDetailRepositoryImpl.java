package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.MiningDetailAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.repository.system.MiningDetailRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.MiningDetailMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MiningDetailRepositoryImpl implements MiningDetailRepository {

    private final MiningDetailMapper miningDetailMapper;
    private final MiningDetailAssembler miningDetailAssembler;

    @Override
    public int updateBatch(List<MiningDetail> list) {
        return miningDetailMapper.updateBatch(miningDetailAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<MiningDetail> list) {
        return miningDetailMapper.updateBatchSelective(miningDetailAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<MiningDetail> list) {
        return miningDetailMapper.batchInsert(miningDetailAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(MiningDetail record) {
        return miningDetailMapper.insertOrUpdate(miningDetailAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(MiningDetail record) {
        return miningDetailMapper.insertOrUpdateSelective(miningDetailAssembler.domain2Po(record));
    }

    @Override
    public int saveOrUpdateBatch(List<MiningDetail> miningDetails) {
        return miningDetailMapper.batchInsertOrUpdate(miningDetailAssembler.domain2Po(miningDetails));
    }
    // TODO: 实现 BaseRepository<MiningDetail, Long> 的方法
} 