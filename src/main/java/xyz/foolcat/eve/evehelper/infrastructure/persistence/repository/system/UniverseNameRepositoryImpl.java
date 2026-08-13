package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.domain.repository.system.UniverseNameRepository;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.UniverseNamePoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.UniverseNamePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.UniverseNameMapper;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class UniverseNameRepositoryImpl implements UniverseNameRepository {

    private final UniverseNameMapper universeNameMapper;
    private final UniverseNamePoConverter universeNamePoConverter;

    @Override
    public int updateBatch(List<UniverseName> list) {
        return universeNameMapper.updateBatch(universeNamePoConverter.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<UniverseName> list) {
        return universeNameMapper.updateBatchSelective(universeNamePoConverter.domain2Po(list));
    }

    @Override
    public int batchInsert(List<UniverseName> list) {
        return universeNameMapper.batchInsert(universeNamePoConverter.domain2Po(list));
    }

    @Override
    public boolean insertOrUpdate(UniverseName record) {
        return universeNameMapper.insertOrUpdate(universeNamePoConverter.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(UniverseName record) {
        return universeNameMapper.insertOrUpdateSelective(universeNamePoConverter.domain2Po(record));
    }

    @Override
    public int saveOrUpdateBatch(List<UniverseName> newUnivereName) {
        return universeNameMapper.batchInsertOrUpdate(universeNamePoConverter.domain2Po(newUnivereName));
    }

    @Override
    public List<UniverseName> selectByIdIn(List<Integer> items) {
        if (items.isEmpty()){
            throw new EveHelperException(ResultCode.SYSTEM_PARAM_IS_NULL);
        }
        return universeNamePoConverter.po2Domain(universeNameMapper.selectList(new QueryWrapper<UniverseNamePO>().lambda()
                .in(UniverseNamePO::getId, items)));
    }
}
