package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.StructurePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.StructureMapper;

import java.util.List;

@Repository
public class StructureRepositoryImpl implements StructureRepository {
    @Autowired
    private StructureMapper structureMapper;

    public int updateBatch(List<StructurePO> list) {
        return structureMapper.updateBatch(list);
    }
    public int updateBatchSelective(List<StructurePO> list) {
        return structureMapper.updateBatchSelective(list);
    }
    public int batchInsert(List<StructurePO> list) {
        return structureMapper.batchInsert(list);
    }
    public int insertOrUpdate(StructurePO record) {
        return structureMapper.insertOrUpdate(record);
    }
    public int insertOrUpdateSelective(StructurePO record) {
        return structureMapper.insertOrUpdateSelective(record);
    }
    public StructurePO selectByStructureId(Long structureId) {
        return structureMapper.selectByStructureId(structureId);
    }
    public List<StructurePO> selectFuelExpiresList(Integer hour, Integer corporationId) {
        return structureMapper.selectFuelExpiresList(hour, corporationId);
    }
    public int batchInsertOrUpdate(List<StructurePO> list) {
        return structureMapper.batchInsertOrUpdate(list);
    }
    // TODO: 实现 BaseRepository<Structure, Long> 的方法
} 