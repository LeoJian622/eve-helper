package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintsAssembler;
import xyz.foolcat.eve.evehelper.application.dto.request.BlueprintsQuery;
import xyz.foolcat.eve.evehelper.domain.model.query.BlueprintsPageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 蓝图应用服务单元测试。
 * 重点验证查询条件被完整传递到领域仓储，而非静默丢弃。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("蓝图应用服务单元测试")
class BlueprintsApplicationServiceUnitTest {

    @Mock
    BlueprintsRepository blueprintsRepository;

    @Mock
    BlueprintsAssembler blueprintsAssembler;

    private BlueprintsApplicationService service;

    @BeforeEach
    void setUp() {
        service = new BlueprintsApplicationService(blueprintsRepository, blueprintsAssembler);
    }

    private void stubRepositoryReturningEmptyPage() {
        when(blueprintsRepository.selectBlueprintsInvtypeUniverse(any(BlueprintsPageCriteria.class)))
                .thenReturn(PageResult.<BlueprintsDTO>builder()
                        .records(List.of())
                        .total(0L)
                        .current(1L)
                        .size(20L)
                        .pages(0L)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());
        when(blueprintsAssembler.dto2Vo(List.of())).thenReturn(List.of());
    }

    private BlueprintsPageCriteria captureCriteria() {
        ArgumentCaptor<BlueprintsPageCriteria> captor = ArgumentCaptor.forClass(BlueprintsPageCriteria.class);
        verify(blueprintsRepository).selectBlueprintsInvtypeUniverse(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("蓝图名称筛选条件传递到仓储，不被静默丢弃")
    void passesBlueprintNameToRepository() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .blueprintName("恶狼级")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals("恶狼级", captureCriteria().getBlueprintName());
    }

    @Test
    @DisplayName("空白蓝图名称归一为 null，避免生成无意义的 like 条件")
    void blankBlueprintNameNormalizedToNull() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .blueprintName("   ")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertNull(captureCriteria().getBlueprintName());
    }

    @Test
    @DisplayName("blueprintType=copy 映射为仅拷贝筛选")
    void mapsCopyTypeFilter() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .blueprintType("copy")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals(BlueprintsPageCriteria.CopyFilter.COPY, captureCriteria().getCopyFilter());
    }

    @Test
    @DisplayName("blueprintType=original 映射为仅原图筛选，且大小写不敏感")
    void mapsOriginalTypeFilterIgnoringCase() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .blueprintType("ORIGINAL")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals(BlueprintsPageCriteria.CopyFilter.ORIGINAL, captureCriteria().getCopyFilter());
    }

    @Test
    @DisplayName("blueprintType 缺省时不筛选原图/拷贝")
    void defaultsToAnyCopyFilter() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("2112832425").build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        assertEquals(BlueprintsPageCriteria.CopyFilter.ANY, captureCriteria().getCopyFilter());
    }

    @Test
    @DisplayName("非法 blueprintType 抛出业务异常，而非静默忽略")
    void rejectsUnknownBlueprintType() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .blueprintType("bpo-or-something")
                .build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("白名单内的排序字段被解析并传递")
    void mapsWhitelistedSortField() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .sortField("typeName")
                .sortOrder("asc")
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        BlueprintsPageCriteria criteria = captureCriteria();
        assertEquals(BlueprintsPageCriteria.SortField.TYPE_NAME, criteria.getSortField());
        assertTrue(criteria.isAscending());
    }

    @Test
    @DisplayName("白名单外的排序字段被拒绝，阻断 SQL 注入入口")
    void rejectsSortFieldOutsideWhitelist() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .sortField("bt.item_id; drop table blueprints--")
                .build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("非法排序方向被拒绝，而非静默降级为降序")
    void rejectsUnknownSortOrder() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .sortField("runs")
                .sortOrder("ascending")
                .build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("所有者ID 必须为数字，避免下推数据库做隐式转换")
    void rejectsNonNumericOwnerId() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("abc").build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }

    @Test
    @DisplayName("分页参数传递到仓储条件")
    void passesPaginationToRepository() {
        // Arrange
        stubRepositoryReturningEmptyPage();
        BlueprintsQuery query = BlueprintsQuery.builder()
                .ownerId("2112832425")
                .current(3)
                .size(50)
                .build();

        // Act
        service.queryBlueprintsByPage(query);

        // Assert
        BlueprintsPageCriteria criteria = captureCriteria();
        assertEquals(3L, criteria.getCurrent());
        assertEquals(50L, criteria.getSize());
    }

    @Test
    @DisplayName("所有者ID 为空时抛出业务异常")
    void rejectsBlankOwnerId() {
        // Arrange
        BlueprintsQuery query = BlueprintsQuery.builder().ownerId("  ").build();

        // Act & Assert
        assertThrows(EveHelperException.class, () -> service.queryBlueprintsByPage(query));
        verify(blueprintsRepository, never()).selectBlueprintsInvtypeUniverse(any());
    }
}
