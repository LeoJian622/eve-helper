package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.StructurePO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface StructureMapper extends BaseMapper<StructurePO> {
    int updateBatch(List<StructurePO> list);

    int updateBatchSelective(List<StructurePO> list);

    int batchInsert(List<StructurePO> list);

    int insertOrUpdate(StructurePO record);

    int insertOrUpdateSelective(StructurePO record);

    List<StructurePO> selectFuelExpiresList(@Param("hour") Integer hour, @Param("corporationId") Integer corporationId);

    int batchInsertOrUpdate(List<StructurePO> list);

    int removeBatchByIds(List<Long> ids);
    // 只保留基础 CRUD
}
