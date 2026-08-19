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

    /**
     * 按军团ID查询建筑(写入侧 stale 删除左源)
     *
     * @param corporationId 军团ID
     * @param userId        所有者归属过滤:null=不过滤(ROOT/管理),非 null=仅该同步者自己的行
     * @return 建筑列表
     */
    List<Structure> selectByCorporationId(Integer corporationId, Long userId);

    /**
     * 分页查询军团建筑列表,关联解析类型名与星系名(只读)
     *
     * @param criteria 分页查询条件(含排序白名单)
     * @param userId   军团维读过滤(US2b):null=ROOT 看全量不过滤,非 null 按 user_id 过滤
     * @return 分页结果
     */
    PageResult<StructureListItemDTO> selectStructuresWithNames(StructurePageCriteria criteria, Long userId);

    /**
     * 查询单建筑详情,关联解析类型名与星系名(只读)
     *
     * @param structureId 建筑ID
     * @param userId      军团维读过滤(US2b-R1):null=ROOT 不过滤,非 null 按 user_id 过滤
     * @return 建筑详情(含 services 原始 JSON),不存在返回 null
     */
    StructureDetailDTO selectDetailById(Long structureId, Long userId);

    /**
     * 查询指定预警时长内燃料耗尽的建筑,关联解析类型名与星系名,返回剩余时长(只读)
     *
     * @param corporationId 军团ID
     * @param hour          预警时长(小时)
     * @param userId        军团维读过滤(US2b):null=ROOT 不过滤,非 null 按 user_id 过滤
     * @return 缺油建筑列表(含剩余时长)
     */
    List<StructureFuelDTO> selectFuelExpiresListWithNames(String corporationId, Integer hour, Long userId);

    /**
     * 查询单建筑服务状态(仅 structureId/name/services,只读)
     *
     * @param structureId 建筑ID
     * @param userId      军团维读过滤(US2b-R1):null=ROOT 不过滤,非 null 按 user_id 过滤
     * @return 建筑服务状态(含 services 原始 JSON),不存在返回 null
     */
    StructureServiceDTO selectServicesById(Long structureId, Long userId);

    /**
     * 统计概览:按状态分组聚合,返回各状态计数与缺油小计(只读,单 SQL SC-003)
     *
     * @param corporationId 军团ID
     * @param userId        军团维读过滤(US2b):null=ROOT 不过滤,非 null 按 user_id 过滤
     * @return 按状态分组的行级统计列表(应用层汇总为概览 VO)
     */
    List<StructureSummaryDTO> selectSummary(String corporationId, Long userId);

    /**
     * 查询增强/解锚时间提醒建筑(只读)
     *
     * @param corporationId 军团ID
     * @param userId        军团维读过滤(US2b):null=ROOT 不过滤,非 null 按 user_id 过滤
     * @return 处于增强窗口或即将解锚的建筑列表
     */
    List<StructureTimerDTO> selectTimers(String corporationId, Long userId);
}
