package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.UniverseNamePO;

import java.util.List;

@Mapper
public interface UniverseNameMapper extends BaseMapper<UniverseNamePO> {
    int updateBatch(List<UniverseNamePO> universeNamePOS);

    int updateBatchSelective(List<UniverseNamePO> universeNamePOS);

    int batchInsert(List<UniverseNamePO> universeNamePOS);

    int insertOrUpdate(UniverseNamePO universeNamePO);

    int insertOrUpdateSelective(UniverseNamePO universeNamePO);

    int batchInsertOrUpdate(List<UniverseNamePO> universeNamePOS);
    // 只保留基础 CRUD
}
