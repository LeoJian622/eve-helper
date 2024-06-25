package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.Structure;

import java.util.List;

/**
 * 建筑信息查询
 * @author Leojan
 */
@Mapper
public interface StructureMapper extends BaseMapper<Structure> {
    int updateBatch(List<Structure> list);

    int updateBatchSelective(List<Structure> list);

    int batchInsert(@Param("list") List<Structure> list);

    int insertOrUpdate(Structure record);

    int insertOrUpdateSelective(Structure record);

    /**
     * 查询X小时后燃料耗尽的建筑
     *
     * @param hour 小时数
     * @return 建筑列表
     */
    List<Structure> selectFuelExpiresList(@Param("hour") Integer hour, @Param("corporationId") Integer corporationId);

    int batchInsertOrUpdate(@Param("list") List<Structure> list);
}