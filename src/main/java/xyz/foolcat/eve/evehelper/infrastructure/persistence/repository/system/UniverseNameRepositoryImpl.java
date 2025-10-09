package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.UniverseNameAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.UniverseName;
import xyz.foolcat.eve.evehelper.domain.repository.system.UniverseNameRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.UniverseNamePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.UniverseNameMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UniverseNameRepositoryImpl implements UniverseNameRepository {

    private final UniverseNameMapper universeNameMapper;
    private final UniverseNameAssembler universeNameAssembler;

    @Override
    public int updateBatch(List<UniverseName> list) {
        return universeNameMapper.updateBatch(universeNameAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<UniverseName> list) {
        return universeNameMapper.updateBatchSelective(universeNameAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<UniverseName> list) {
        return universeNameMapper.batchInsert(universeNameAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(UniverseName record) {
        return universeNameMapper.insertOrUpdate(universeNameAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(UniverseName record) {
        return universeNameMapper.insertOrUpdateSelective(universeNameAssembler.domain2Po(record));
    }

    @Override
    public int saveOrUpdateBatch(List<UniverseName> newUnivereName) {
        return universeNameMapper.batchInsertOrUpdate(universeNameAssembler.domain2Po(newUnivereName));
    }

    @Override
    public List<UniverseName> selectByIdIn(List<Integer> items) {
        return universeNameAssembler.po2Domain(universeNameMapper.selectList(new QueryWrapper<UniverseNamePO>().lambda()
                .in(UniverseNamePO::getId, items)));
    }
    // TODO: 实现 BaseRepository<UniverseName, Long> 的方法
} 