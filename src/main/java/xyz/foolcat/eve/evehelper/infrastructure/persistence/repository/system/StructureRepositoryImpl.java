package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.application.assembler.system.StructureAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.StructurePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.StructureMapper;

import java.util.List;

/**
 * @author Leojan
 */
@Repository
@RequiredArgsConstructor
public class StructureRepositoryImpl implements StructureRepository {

    private final StructureMapper structureMapper;

    private final StructureAssembler structureAssembler;

    @Override
    public int updateBatch(List<Structure> list) {
        return structureMapper.updateBatch(structureAssembler.domain2Po(list));
    }

    @Override
    public int updateBatchSelective(List<Structure> list) {
        return structureMapper.updateBatchSelective(structureAssembler.domain2Po(list));
    }

    @Override
    public int batchInsert(List<Structure> list) {
        return structureMapper.batchInsert(structureAssembler.domain2Po(list));
    }

    @Override
    public int insertOrUpdate(Structure record) {
        return structureMapper.insertOrUpdate(structureAssembler.domain2Po(record));
    }

    @Override
    public int insertOrUpdateSelective(Structure record) {
        return structureMapper.insertOrUpdateSelective(structureAssembler.domain2Po(record));
    }

    @Override
    public Structure selectByStructureId(Long structureId) {
        return structureAssembler.po2Domain(structureMapper.selectById(structureId));
    }

    @Override
    public List<Structure> selectFuelExpiresList(Integer hour, Integer corporationId) {
        return structureAssembler.po2Domain(structureMapper.selectFuelExpiresList(hour, corporationId));
    }

    @Override
    public int removeBatchByIds(List<Long> ids) {
        return structureMapper.removeBatchByIds(ids);
    }

    @Override
    public List<Structure> selectByCorporationId(Integer corporationId) {
        return structureAssembler.po2Domain(structureMapper.selectList(new QueryWrapper<StructurePO>().lambda().eq(StructurePO::getCorporationId, corporationId)));
    }

    @Override
    public int batchInsertOrUpdate(List<Structure> list) {
        return structureMapper.batchInsertOrUpdate(structureAssembler.domain2Po(list));
    }
    // TODO: 实现 BaseRepository<Structure, Long> 的方法
} 