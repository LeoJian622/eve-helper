package xyz.foolcat.eve.evehelper.application.service;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.StructureAssembler;
import xyz.foolcat.eve.evehelper.application.dto.request.StructureFuelQuery;
import xyz.foolcat.eve.evehelper.application.dto.request.StructureQuery;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureDetailVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureFuelVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureListItemVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureServiceVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureSummaryVO;
import xyz.foolcat.eve.evehelper.application.dto.response.StructureTimerVO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.StructureService;
import xyz.foolcat.eve.evehelper.domain.model.query.StructurePageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureDetailDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureServiceDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureSummaryDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureTimerDTO;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 建筑查询应用服务(只读)
 * 负责建筑查询用例编排与 IDOR 归属校验
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StructureQueryApplicationService {

    /**
     * 允许的排序字段:请求参数名 -> 领域排序字段(白名单,防注入 FR-015)
     */
    private static final Map<String, StructurePageCriteria.SortField> SORT_FIELDS = Map.of(
            "structureid", StructurePageCriteria.SortField.STRUCTURE_ID,
            "name", StructurePageCriteria.SortField.NAME,
            "state", StructurePageCriteria.SortField.STATE,
            "fuelexpires", StructurePageCriteria.SortField.FUEL_EXPIRES);

    /**
     * 军团ID 必须为纯数字(库中为 BIGINT)
     */
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    /**
     * 军团ID 最大位数,BIGINT 上限为 19 位
     */
    private static final int CORPORATION_ID_MAX_LENGTH = 19;

    private final StructureRepository structureRepository;

    private final StructureAssembler structureAssembler;

    private final AccessGuard accessGuard;

    /**
     * 分页查询军团建筑列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public PageResult<StructureListItemVO> queryStructuresByPage(StructureQuery query) {
        String corporationId = requireCorporationId(query.getCorporationId());
        // 归属校验先于其余参数解析,避免越权请求探测参数校验细节(FR-010)
        accessGuard.requireOwnership(corporationId, "建筑");
        StructurePageCriteria criteria = toCriteria(query, corporationId);
        return PageResultUtil.copy(
                structureRepository.selectStructuresWithNames(criteria),
                structureAssembler::dtoList2VoList);
    }

    /**
     * 查询单建筑详情,校验军团归属与建筑归属,容错解析服务列表(FR-005/011/014)
     *
     * @param corpId      军团ID(请求路径)
     * @param structureId 建筑ID
     * @return 建筑详情(含已解析服务列表)
     */
    public StructureDetailVO queryDetailById(String corpId, Long structureId) {
        String corporationId = requireCorporationId(corpId);
        accessGuard.requireOwnership(corporationId, "建筑");
        StructureDetailDTO dto = structureRepository.selectDetailById(structureId);
        // 防枚举(L1):建筑不存在与无权访问返回同一错误,避免攻击者枚举建筑 ID
        if (dto == null || !corporationId.equals(String.valueOf(dto.getCorporationId()))) {
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
        StructureDetailVO vo = structureAssembler.dto2Vo(dto);
        vo.setServices(parseServices(dto.getServicesJson()));
        return vo;
    }

    /**
     * 容错解析建筑服务 JSON,失败或为空返回空列表(FR-014, R-01)
     */
    private List<StructureService> parseServices(String servicesJson) {
        if (servicesJson == null || servicesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<StructureService> services = JSONUtil.toList(servicesJson, StructureService.class);
            return services == null ? Collections.emptyList() : services;
        } catch (Exception e) {
            log.warn("解析建筑服务 JSON 失败,返回空列表:{}", servicesJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询燃料即将耗尽的建筑,支持预警时长参数(FR-006)
     *
     * @param query 燃料预警查询条件(含预警时长)
     * @return 缺油建筑列表(含剩余时长)
     */
    public List<StructureFuelVO> queryFuelExpiring(StructureFuelQuery query) {
        String corporationId = requireCorporationId(query.getCorporationId());
        accessGuard.requireOwnership(corporationId, "建筑");
        int hours = query.getHours() == null ? 72 : query.getHours();
        return structureAssembler.fuelDtoList2VoList(
                structureRepository.selectFuelExpiresListWithNames(corporationId, hours));
    }

    /**
     * 查询单建筑服务状态,校验军团归属与建筑归属,容错解析服务列表(FR-007/011/014)
     *
     * @param corpId      军团ID(请求路径)
     * @param structureId 建筑ID
     * @return 建筑服务状态(含已解析服务列表)
     */
    public StructureServiceVO queryServices(String corpId, Long structureId) {
        String corporationId = requireCorporationId(corpId);
        accessGuard.requireOwnership(corporationId, "建筑");
        StructureServiceDTO dto = structureRepository.selectServicesById(structureId);
        // 防枚举(L1):建筑不存在与无权访问返回同一错误,避免攻击者枚举建筑 ID
        if (dto == null || !corporationId.equals(String.valueOf(dto.getCorporationId()))) {
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
        StructureServiceVO vo = structureAssembler.dto2Vo(dto);
        vo.setServices(parseServices(dto.getServicesJson()));
        return vo;
    }

    /**
     * 统计概览,单 SQL 聚合后应用层汇总(FR-008, SC-003)
     *
     * @param corpId 军团ID(请求路径)
     * @return 建筑统计概览(总数/缺油数/即将缺油数/各状态计数)
     */
    public StructureSummaryVO querySummary(String corpId) {
        String corporationId = requireCorporationId(corpId);
        accessGuard.requireOwnership(corporationId, "建筑");
        List<StructureSummaryDTO> rows = structureRepository.selectSummary(corporationId);
        Map<String, Long> stateCounts = new LinkedHashMap<>();
        long total = 0L;
        long fuelExpiredCount = 0L;
        long lowFuelCount = 0L;
        for (StructureSummaryDTO row : rows) {
            long count = row.getStateCount() == null ? 0L : row.getStateCount();
            String state = row.getState();
            if (state != null) {
                stateCounts.put(state, count);
            }
            total += count;
            fuelExpiredCount += row.getFuelExpiredCount() == null ? 0L : row.getFuelExpiredCount();
            lowFuelCount += row.getLowFuelCount() == null ? 0L : row.getLowFuelCount();
        }
        StructureSummaryVO vo = new StructureSummaryVO();
        vo.setTotal(total);
        vo.setFuelExpiredCount(fuelExpiredCount);
        vo.setLowFuelCount(lowFuelCount);
        vo.setStateCounts(stateCounts);
        return vo;
    }

    /**
     * 查询增强/解锚时间提醒建筑(FR-009)
     *
     * @param corpId 军团ID(请求路径)
     * @return 处于增强窗口(state 含 vulnerable)或即将解锚(近 7 天)的建筑列表
     */
    public List<StructureTimerVO> queryTimers(String corpId) {
        String corporationId = requireCorporationId(corpId);
        accessGuard.requireOwnership(corporationId, "建筑");
        return structureAssembler.timerDtoList2VoList(
                structureRepository.selectTimers(corporationId));
    }

    private StructurePageCriteria toCriteria(StructureQuery query, String corporationId) {
        return StructurePageCriteria.builder()
                .corporationId(corporationId)
                .name(normalizeName(query.getName()))
                .state(normalizeState(query.getState()))
                .lowFuelOnly(Boolean.TRUE.equals(query.getLowFuelOnly()))
                .sortField(parseSortField(query.getSortField()))
                .ascending(isAscending(query.getSortOrder()))
                .current(query.getCurrent())
                .size(query.getSize())
                .build();
    }

    private String requireCorporationId(String corporationId) {
        if (corporationId == null || corporationId.isBlank()) {
            throw new EveHelperException("军团ID不能为空");
        }
        String trimmed = corporationId.trim();
        // corporation_id 在库中为 BIGINT,非数字会下推数据库做隐式转换导致索引失效
        if (!DIGITS.matcher(trimmed).matches() || trimmed.length() > CORPORATION_ID_MAX_LENGTH) {
            throw new EveHelperException("军团ID必须为不超过 " + CORPORATION_ID_MAX_LENGTH + " 位的数字:" + corporationId);
        }
        return trimmed;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private String normalizeState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        return state.trim();
    }

    private StructurePageCriteria.SortField parseSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return null;
        }
        StructurePageCriteria.SortField resolved =
                SORT_FIELDS.get(sortField.trim().toLowerCase(Locale.ROOT).replace("_", ""));
        if (resolved == null) {
            throw new EveHelperException("不支持的排序字段:" + sortField);
        }
        return resolved;
    }

    private boolean isAscending(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return false;
        }
        String normalized = sortOrder.trim().toLowerCase(Locale.ROOT);
        if ("asc".equals(normalized)) {
            return true;
        }
        if ("desc".equals(normalized)) {
            return false;
        }
        throw new EveHelperException("不支持的排序方向:" + sortOrder + ",可选值 asc、desc");
    }
}
