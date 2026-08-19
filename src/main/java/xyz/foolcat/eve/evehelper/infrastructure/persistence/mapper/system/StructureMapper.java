package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureDetailDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureFuelDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureListItemDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureServiceDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureSummaryDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureTimerDTO;
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

    int insertOrUpdateSelective(StructurePO record);

    List<StructurePO> selectFuelExpiresList(@Param("hour") Integer hour, @Param("corporationId") Integer corporationId);

    int batchInsertOrUpdate(List<StructurePO> list);

    int removeBatchByIds(List<Long> ids);

    /**
     * 分页查询建筑列表,关联 inv_types(类型名)与 universe_name(星系名)
     */
    IPage<StructureListItemDTO> selectStructuresWithNames(
            IPage<StructureListItemDTO> page,
            @Param("corporationId") String corporationId,
            @Param("name") String name,
            @Param("state") String state,
            @Param("lowFuelOnly") boolean lowFuelOnly,
            @Param("sortColumn") String sortColumn,
            @Param("ascending") boolean ascending,
            @Param("userId") Long userId);

    /**
     * 查询单建筑详情,关联 inv_types(类型名)与 universe_name(星系名)
     */
    StructureDetailDTO selectDetailById(@Param("structureId") Long structureId);

    /**
     * 查询指定预警时长内燃料耗尽的建筑,关联类型名/星系名,返回剩余时长
     */
    List<StructureFuelDTO> selectFuelExpiresListWithNames(
            @Param("corporationId") String corporationId,
            @Param("hour") Integer hour,
            @Param("userId") Long userId);

    /**
     * 查询单建筑服务状态(仅 structureId/corporationId/name/services)
     */
    StructureServiceDTO selectServicesById(@Param("structureId") Long structureId);

    /**
     * 统计概览:按状态分组聚合(GROUP BY state)
     */
    List<StructureSummaryDTO> selectSummary(@Param("corporationId") String corporationId,
                                            @Param("userId") Long userId);

    /**
     * 查询增强/解锚时间提醒建筑
     */
    List<StructureTimerDTO> selectTimers(@Param("corporationId") String corporationId,
                                         @Param("userId") Long userId);
}
