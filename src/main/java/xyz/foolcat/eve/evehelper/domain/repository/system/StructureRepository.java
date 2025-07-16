package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;

import java.util.List;

public interface StructureRepository {

    int updateBatch(List<Structure> list);

    int updateBatchSelective(List<Structure> list);

    int batchInsert(List<Structure> list);

    int insertOrUpdate(Structure record);

    int insertOrUpdateSelective(Structure record);

    int batchInsertOrUpdate(List<Structure> list);

    /**
     * 查询X小时后燃料耗尽的建筑
     *
     * @param hour 小时数
     * @return 建筑列表
     */
    List<Structure> selectFuelExpiresList(Integer hour, Integer corporationId);
} 