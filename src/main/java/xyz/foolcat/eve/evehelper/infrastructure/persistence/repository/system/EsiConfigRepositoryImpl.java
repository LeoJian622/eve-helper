package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.EsiConfigAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EsiConfig;
import xyz.foolcat.eve.evehelper.domain.repository.system.EsiConfigRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EsiConfigMapper;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class EsiConfigRepositoryImpl implements EsiConfigRepository {

    @Resource
    private EsiConfigMapper esiConfigMapper;

    @Resource
    private EsiConfigAssembler esiConfigAssembler;

    @Override
    public int updateBatch(List<EsiConfig> list) {
        return esiConfigMapper.updateBatch(esiConfigAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<EsiConfig> list) {
        return esiConfigMapper.updateBatchSelective(esiConfigAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<EsiConfig> list) {
        return esiConfigMapper.batchInsert(esiConfigAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(EsiConfig record) {
        return esiConfigMapper.insertOrUpdate(esiConfigAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(EsiConfig record) {
        return esiConfigMapper.insertOrUpdateSelective(esiConfigAssembler.domain2Po(record));
    }
    // TODO: 实现 BaseRepository<EsiConfig, Long> 的方法
} 