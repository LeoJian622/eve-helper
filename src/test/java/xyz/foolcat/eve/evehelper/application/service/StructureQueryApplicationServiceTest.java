package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import xyz.foolcat.eve.evehelper.domain.model.query.StructurePageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureDetailDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureFuelDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureListItemDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureServiceDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureSummaryDTO;
import xyz.foolcat.eve.evehelper.domain.model.vo.StructureTimerDTO;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 建筑查询应用服务单元测试(US1~US6)
 * mock 仓储/装配器/AccessGuard,验证参数校验、排序白名单、IDOR、JSON 容错、汇总逻辑
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("建筑查询应用服务单元测试")
class StructureQueryApplicationServiceTest {

    private static final String CORP_ID = "98000001";
    private static final long CORP_ID_NUM = 98000001L;
    private static final long STRUCTURE_ID = 100L;

    @Mock
    StructureRepository structureRepository;

    @Mock
    StructureAssembler structureAssembler;

    @Mock
    AccessGuard accessGuard;

    @InjectMocks
    StructureQueryApplicationService service;

    // ===== 参数校验 requireCorporationId(先于 IDOR,防越权探测)=====

    @Test
    @DisplayName("corporationId 为空抛异常,不调用 AccessGuard")
    void queryStructuresByPage_blankCorpId_throwsBeforeAccessGuard() {
        StructureQuery q = StructureQuery.builder().corporationId("").build();
        assertThrows(EveHelperException.class, () -> service.queryStructuresByPage(q));
        verifyNoInteractions(accessGuard);
    }

    @Test
    @DisplayName("corporationId 非数字抛异常,不调用 AccessGuard")
    void queryStructuresByPage_nonDigitCorpId_throwsBeforeAccessGuard() {
        StructureQuery q = StructureQuery.builder().corporationId("abc").build();
        assertThrows(EveHelperException.class, () -> service.queryStructuresByPage(q));
        verifyNoInteractions(accessGuard);
    }

    @Test
    @DisplayName("corporationId 超过19位抛异常,不调用 AccessGuard")
    void queryStructuresByPage_tooLongCorpId_throwsBeforeAccessGuard() {
        StructureQuery q = StructureQuery.builder().corporationId("12345678901234567890").build();
        assertThrows(EveHelperException.class, () -> service.queryStructuresByPage(q));
        verifyNoInteractions(accessGuard);
    }

    // ===== 排序白名单(FR-015)=====

    @Test
    @DisplayName("非白名单排序字段抛异常")
    void queryStructuresByPage_invalidSortField_throws() {
        StructureQuery q = StructureQuery.builder().corporationId(CORP_ID).sortField("password").build();
        assertThrows(EveHelperException.class, () -> service.queryStructuresByPage(q));
    }

    @Test
    @DisplayName("非法排序方向抛异常")
    void queryStructuresByPage_invalidSortOrder_throws() {
        StructureQuery q = StructureQuery.builder().corporationId(CORP_ID).sortOrder("random").build();
        assertThrows(EveHelperException.class, () -> service.queryStructuresByPage(q));
    }

    @Test
    @DisplayName("排序字段归一化匹配白名单(fuelExpires -> FUEL_EXPIRES,asc)")
    void queryStructuresByPage_sortFieldNormalizedToWhitelist() {
        StructureQuery q = StructureQuery.builder().corporationId(CORP_ID)
                .sortField("fuelExpires").sortOrder("asc").current(1).size(20).build();
        when(structureRepository.selectStructuresWithNames(any()))
                .thenReturn(PageResult.<StructureListItemDTO>builder().records(Collections.emptyList()).total(0L).build());
        when(structureAssembler.dtoList2VoList(any())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.queryStructuresByPage(q));

        ArgumentCaptor<StructurePageCriteria> captor = ArgumentCaptor.forClass(StructurePageCriteria.class);
        verify(structureRepository).selectStructuresWithNames(captor.capture());
        assertEquals(StructurePageCriteria.SortField.FUEL_EXPIRES, captor.getValue().getSortField());
        assertTrue(captor.getValue().isAscending());
    }

    // ===== IDOR(FR-010)=====

    @Test
    @DisplayName("AccessGuard 拒绝时传播越权异常,不查仓储")
    void queryStructuresByPage_accessDenied_propagatesAndSkipsRepo() {
        StructureQuery q = StructureQuery.builder().corporationId(CORP_ID).build();
        org.mockito.Mockito.doThrow(new EveHelperException("访问未授权"))
                .when(accessGuard).requireOwnership(CORP_ID, "建筑");

        assertThrows(EveHelperException.class, () -> service.queryStructuresByPage(q));
        verify(structureRepository, never()).selectStructuresWithNames(any());
    }

    // ===== queryStructuresByPage 正常 =====

    @Test
    @DisplayName("正常分页查询返回结果并校验归属")
    void queryStructuresByPage_normal_returnsPagedResult() {
        StructureQuery q = StructureQuery.builder().corporationId(CORP_ID).current(1).size(20).build();
        StructureListItemDTO dto = new StructureListItemDTO();
        dto.setStructureId(STRUCTURE_ID);
        when(structureRepository.selectStructuresWithNames(any()))
                .thenReturn(PageResult.<StructureListItemDTO>builder().records(List.of(dto)).total(1L).build());
        when(structureAssembler.dtoList2VoList(any())).thenReturn(List.of(new StructureListItemVO()));

        PageResult<StructureListItemVO> result = service.queryStructuresByPage(q);

        assertEquals(1, result.getRecords().size());
        verify(accessGuard).requireOwnership(CORP_ID, "建筑");
    }

    // ===== queryDetailById(FR-005/011/014)=====

    @Test
    @DisplayName("建筑不存在抛越权异常(防枚举 L1,与归属不匹配同响应)")
    void queryDetailById_notFound_throws() {
        when(structureRepository.selectDetailById(STRUCTURE_ID)).thenReturn(null);

        assertThrows(EveHelperException.class, () -> service.queryDetailById(CORP_ID, STRUCTURE_ID));
        verify(structureAssembler, never()).dto2Vo(any(StructureDetailDTO.class));
    }

    @Test
    @DisplayName("建筑归属不匹配抛越权异常(FR-011 跨军团防护)")
    void queryDetailById_corpMismatch_throws() {
        StructureDetailDTO dto = new StructureDetailDTO();
        dto.setCorporationId(99999999L);
        when(structureRepository.selectDetailById(STRUCTURE_ID)).thenReturn(dto);

        assertThrows(EveHelperException.class, () -> service.queryDetailById(CORP_ID, STRUCTURE_ID));
    }

    @Test
    @DisplayName("详情正常返回并解析 services JSON")
    void queryDetailById_normal_parsesServices() {
        StructureDetailDTO dto = new StructureDetailDTO();
        dto.setCorporationId(CORP_ID_NUM);
        dto.setServicesJson("[{\"name\":\"克隆\",\"state\":\"online\"}]");
        StructureDetailVO vo = new StructureDetailVO();
        when(structureRepository.selectDetailById(STRUCTURE_ID)).thenReturn(dto);
        when(structureAssembler.dto2Vo(any(StructureDetailDTO.class))).thenReturn(vo);

        StructureDetailVO result = service.queryDetailById(CORP_ID, STRUCTURE_ID);

        assertNotNull(result.getServices());
        assertEquals(1, result.getServices().size());
    }

    @Test
    @DisplayName("详情 services JSON 损坏返回空列表(FR-014 容错)")
    void queryDetailById_corruptJson_returnsEmptyList() {
        StructureDetailDTO dto = new StructureDetailDTO();
        dto.setCorporationId(CORP_ID_NUM);
        dto.setServicesJson("{corrupt");
        StructureDetailVO vo = new StructureDetailVO();
        when(structureRepository.selectDetailById(STRUCTURE_ID)).thenReturn(dto);
        when(structureAssembler.dto2Vo(any(StructureDetailDTO.class))).thenReturn(vo);

        StructureDetailVO result = service.queryDetailById(CORP_ID, STRUCTURE_ID);

        assertTrue(result.getServices().isEmpty());
    }

    @Test
    @DisplayName("详情 services 为空字符串返回空列表")
    void queryDetailById_blankJson_returnsEmptyList() {
        StructureDetailDTO dto = new StructureDetailDTO();
        dto.setCorporationId(CORP_ID_NUM);
        dto.setServicesJson("");
        StructureDetailVO vo = new StructureDetailVO();
        when(structureRepository.selectDetailById(STRUCTURE_ID)).thenReturn(dto);
        when(structureAssembler.dto2Vo(any(StructureDetailDTO.class))).thenReturn(vo);

        StructureDetailVO result = service.queryDetailById(CORP_ID, STRUCTURE_ID);

        assertTrue(result.getServices().isEmpty());
    }

    // ===== queryServices(FR-007/011/014)=====

    @Test
    @DisplayName("服务状态建筑不存在抛越权异常(防枚举 L1,与归属不匹配同响应)")
    void queryServices_notFound_throws() {
        when(structureRepository.selectServicesById(STRUCTURE_ID)).thenReturn(null);

        assertThrows(EveHelperException.class, () -> service.queryServices(CORP_ID, STRUCTURE_ID));
    }

    @Test
    @DisplayName("服务状态归属不匹配抛越权异常")
    void queryServices_corpMismatch_throws() {
        StructureServiceDTO dto = new StructureServiceDTO();
        dto.setCorporationId(99999999L);
        when(structureRepository.selectServicesById(STRUCTURE_ID)).thenReturn(dto);

        assertThrows(EveHelperException.class, () -> service.queryServices(CORP_ID, STRUCTURE_ID));
    }

    @Test
    @DisplayName("服务状态正常返回并解析 services")
    void queryServices_normal_parsesServices() {
        StructureServiceDTO dto = new StructureServiceDTO();
        dto.setCorporationId(CORP_ID_NUM);
        dto.setServicesJson("[{\"name\":\"市场\",\"state\":\"online\"}]");
        StructureServiceVO vo = new StructureServiceVO();
        when(structureRepository.selectServicesById(STRUCTURE_ID)).thenReturn(dto);
        when(structureAssembler.dto2Vo(any(StructureServiceDTO.class))).thenReturn(vo);

        StructureServiceVO result = service.queryServices(CORP_ID, STRUCTURE_ID);

        assertEquals(1, result.getServices().size());
    }

    // ===== queryFuelExpiring(FR-006)=====

    @Test
    @DisplayName("hours 为 null 时默认 72 小时")
    void queryFuelExpiring_nullHours_defaultsTo72() {
        StructureFuelQuery q = StructureFuelQuery.builder().corporationId(CORP_ID).hours(null).build();
        when(structureRepository.selectFuelExpiresListWithNames(CORP_ID, 72)).thenReturn(Collections.emptyList());
        when(structureAssembler.fuelDtoList2VoList(any())).thenReturn(Collections.emptyList());

        service.queryFuelExpiring(q);

        verify(structureRepository).selectFuelExpiresListWithNames(CORP_ID, 72);
    }

    @Test
    @DisplayName("燃料预警正常返回")
    void queryFuelExpiring_normal_returnsList() {
        StructureFuelQuery q = StructureFuelQuery.builder().corporationId(CORP_ID).hours(48).build();
        when(structureRepository.selectFuelExpiresListWithNames(CORP_ID, 48))
                .thenReturn(List.of(new StructureFuelDTO()));
        when(structureAssembler.fuelDtoList2VoList(any())).thenReturn(List.of(new StructureFuelVO()));

        List<StructureFuelVO> result = service.queryFuelExpiring(q);

        assertEquals(1, result.size());
    }

    // ===== querySummary(FR-008, SC-003)=====

    @Test
    @DisplayName("统计汇总:多状态行级汇总为总数/缺油/即将缺油/状态计数")
    void querySummary_multiState_aggregatesCorrectly() {
        StructureSummaryDTO row1 = new StructureSummaryDTO();
        row1.setState("shield");
        row1.setStateCount(5L);
        row1.setFuelExpiredCount(1L);
        row1.setLowFuelCount(2L);
        StructureSummaryDTO row2 = new StructureSummaryDTO();
        row2.setState("armor");
        row2.setStateCount(3L);
        row2.setFuelExpiredCount(0L);
        row2.setLowFuelCount(1L);
        when(structureRepository.selectSummary(CORP_ID)).thenReturn(List.of(row1, row2));

        StructureSummaryVO vo = service.querySummary(CORP_ID);

        assertEquals(8L, vo.getTotal());
        assertEquals(1L, vo.getFuelExpiredCount());
        assertEquals(3L, vo.getLowFuelCount());
        assertEquals(2, vo.getStateCounts().size());
        assertEquals(5L, vo.getStateCounts().get("shield"));
        assertEquals(3L, vo.getStateCounts().get("armor"));
    }

    @Test
    @DisplayName("统计 null 安全:计数为 null 视为 0,state 为 null 不入 stateCounts 但计入 total")
    void querySummary_nullSafe_treatsNullAsZero() {
        StructureSummaryDTO row = new StructureSummaryDTO();
        row.setState(null);
        row.setStateCount(null);
        row.setFuelExpiredCount(null);
        row.setLowFuelCount(null);
        when(structureRepository.selectSummary(CORP_ID)).thenReturn(List.of(row));

        StructureSummaryVO vo = service.querySummary(CORP_ID);

        assertEquals(0L, vo.getTotal());
        assertEquals(0L, vo.getFuelExpiredCount());
        assertEquals(0L, vo.getLowFuelCount());
        assertTrue(vo.getStateCounts().isEmpty());
    }

    @Test
    @DisplayName("统计空列表返回全 0")
    void querySummary_emptyList_returnsZeros() {
        when(structureRepository.selectSummary(CORP_ID)).thenReturn(Collections.emptyList());

        StructureSummaryVO vo = service.querySummary(CORP_ID);

        assertEquals(0L, vo.getTotal());
        assertTrue(vo.getStateCounts().isEmpty());
    }

    // ===== queryTimers(FR-009)=====

    @Test
    @DisplayName("时间提醒正常返回并校验归属")
    void queryTimers_normal_returnsList() {
        when(structureRepository.selectTimers(CORP_ID)).thenReturn(List.of(new StructureTimerDTO()));
        when(structureAssembler.timerDtoList2VoList(any())).thenReturn(List.of(new StructureTimerVO()));

        List<StructureTimerVO> result = service.queryTimers(CORP_ID);

        assertEquals(1, result.size());
        verify(accessGuard).requireOwnership(CORP_ID, "建筑");
    }
}
