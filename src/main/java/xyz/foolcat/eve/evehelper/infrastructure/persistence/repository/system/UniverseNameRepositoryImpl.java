package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import xyz.foolcat.eve.evehelper.domain.repository.system.UniverseNameRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.UniverseNamePO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.UniverseNameMapper;

import java.util.List;

@Repository
public class UniverseNameRepositoryImpl implements UniverseNameRepository {
    @Autowired
    private UniverseNameMapper universeNameMapper;

    public int updateBatch(List<UniverseNamePO> list) {
        return universeNameMapper.updateBatch(list);
    }
    public int updateBatchSelective(List<UniverseNamePO> list) {
        return universeNameMapper.updateBatchSelective(list);
    }
    public int batchInsert(List<UniverseNamePO> list) {
        return universeNameMapper.batchInsert(list);
    }
    public int insertOrUpdate(UniverseNamePO record) {
        return universeNameMapper.insertOrUpdate(record);
    }
    public int insertOrUpdateSelective(UniverseNamePO record) {
        return universeNameMapper.insertOrUpdateSelective(record);
    }
    // TODO: 实现 BaseRepository<UniverseName, Long> 的方法
} 