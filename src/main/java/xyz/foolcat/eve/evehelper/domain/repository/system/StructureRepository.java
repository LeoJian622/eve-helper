package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.model.query.StructurePageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureDetailDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureFuelDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureListItemDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureServiceDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureSummaryDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureTimerDTO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.List;

/**
 * @author Leojan
 */
public interface StructureRepository {

    int updateBatch(List<Structure> list);

    int updateBatchSelective(List<Structure> list);

    int batchInsert(List<Structure> list);

    public boolean insertOrUpdate(Structure record);

    int insertOrUpdateSelective(Structure record);

    int batchInsertOrUpdate(List<Structure> list);

    Structure selectByStructureId(Long structureId);

    /**
     * 查询X小时后燃料耗尽的建筑
     *
     * @param hour 小时数
     * @return 建筑列表
     */
    List<Structure> selectFuelExpiresList(Integer hour, Integer corporationId);

    int removeBatchByIds(List<Long> ids);

    List<Structure> selectByCorporationId(Integer corporationId);

    /**
     * 分页查询军团建筑列表,关联解析类型名与星系名(只读)
     *
     * @param criteria 分页查询条件(含排序白名单)
     * @return 分页结果
     */
    PageResult<StructureListItemDTO> selectStructuresWithNames(StructurePageCriteria criteria);

    /**
     * 查询单建筑详情,关联解析类型名与星系名(只读)
     *
     * @param structureId 建筑ID
     * @return 建筑详情(含 services 原始 JSON),不存在返回 null
     */
    StructureDetailDTO selectDetailById(Long structureId);

    /**
     * 查询指定预警时长内燃料耗尽的建筑,关联解析类型名与星系名,返回剩余时长(只读)
     *
     * @param corporationId 军团ID
     * @param hour          预警时长(小时)
     * @return 缺油建筑列表(含剩余时长)
     */
    List<StructureFuelDTO> selectFuelExpiresListWithNames(String corporationId, Integer hour);

    /**
     * 查询单建筑服务状态(仅 structureId/name/services,只读)
     *
     * @param structureId 建筑ID
     * @return 建筑服务状态(含 services 原始 JSON),不存在返回 null
     */
    StructureServiceDTO selectServicesById(Long structureId);

    /**
     * 统计概览:按状态分组聚合,返回各状态计数与缺油小计(只读,单 SQL SC-003)
     *
     * @param corporationId 军团ID
     * @return 按状态分组的行级统计列表(应用层汇总为概览 VO)
     */
    List<StructureSummaryDTO> selectSummary(String corporationId);

    /**
     * 查询增强/解锚时间提醒建筑(只读)
     *
     * @param corporationId 军团ID
     * @return 处于增强窗口或即将解锚的建筑列表
     */
    List<StructureTimerDTO> selectTimers(String corporationId);
}
