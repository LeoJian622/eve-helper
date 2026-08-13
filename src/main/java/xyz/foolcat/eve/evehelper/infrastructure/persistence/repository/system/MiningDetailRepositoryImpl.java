package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.MiningDetail;
import xyz.foolcat.eve.evehelper.domain.repository.system.MiningDetailRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.MiningDetailPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.MiningDetailMapper;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class MiningDetailRepositoryImpl implements MiningDetailRepository {

    private final MiningDetailMapper miningDetailMapper;
    private final MiningDetailPoConverter miningDetailPoConverter;

    @Override
    public int updateBatch(List<MiningDetail> list) {
        return miningDetailMapper.updateBatch(miningDetailPoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<MiningDetail> list) {
        return miningDetailMapper.updateBatchSelective(miningDetailPoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<MiningDetail> list) {
        return miningDetailMapper.batchInsert(miningDetailPoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(MiningDetail record) {
        return miningDetailMapper.insertOrUpdate(miningDetailPoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(MiningDetail record) {
        return miningDetailMapper.insertOrUpdateSelective(miningDetailPoConverter.domain2Po(record));
    }

    @Override
    public int saveOrUpdateBatch(List<MiningDetail> miningDetails) {
        if (miningDetails.isEmpty()){
            throw new EveHelperException(ResultCode.SYSTEM_PARAM_IS_NULL);
        }
        return miningDetailMapper.batchInsertOrUpdate(miningDetailPoConverter.domain2Po(miningDetails));
    }
}
