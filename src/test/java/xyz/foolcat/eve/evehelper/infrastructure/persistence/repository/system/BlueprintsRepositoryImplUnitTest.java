package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.query.BlueprintsPageCriteria;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence.BlueprintsPoConverter;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 蓝图仓储实现单元测试。
 * 重点验证 CopyFilter 到数据库标志位的语义映射不被搞反。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("蓝图仓储实现单元测试")
class BlueprintsRepositoryImplUnitTest {

    @MockBean
    BlueprintsMapper blueprintsMapper;

    @MockBean
    BlueprintsPoConverter blueprintsPoConverter;

    @Autowired
    private BlueprintsRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        when(blueprintsMapper.selectBlueprintsInvtypeUniverse(
                any(), anyString(), any(), any(), any(), anyBoolean()))
                .thenReturn(new Page<>(1, 20));
    }

    /**
     * 执行查询并捕获传给 mapper 的 is_blueprint_copy 标志位
     */
    private Boolean captureIsBlueprintCopy(BlueprintsPageCriteria.CopyFilter copyFilter) {
        repository.selectBlueprintsInvtypeUniverse(BlueprintsPageCriteria.builder()
                .ownerId("2112832425")
                .copyFilter(copyFilter)
                .build());

        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        verify(blueprintsMapper).selectBlueprintsInvtypeUniverse(
                any(), anyString(), any(), captor.capture(), any(), anyBoolean());
        return captor.getValue();
    }

    @Test
    @DisplayName("COPY 映射为 true")
    void copyMapsToTrue() {
        assertEquals(Boolean.TRUE, captureIsBlueprintCopy(BlueprintsPageCriteria.CopyFilter.COPY));
    }

    @Test
    @DisplayName("ORIGINAL 映射为 false")
    void originalMapsToFalse() {
        assertEquals(Boolean.FALSE, captureIsBlueprintCopy(BlueprintsPageCriteria.CopyFilter.ORIGINAL));
    }

    @Test
    @DisplayName("ANY 映射为 null 表示不筛选")
    void anyMapsToNull() {
        assertNull(captureIsBlueprintCopy(BlueprintsPageCriteria.CopyFilter.ANY));
    }

    @Test
    @DisplayName("排序字段以枚举硬编码列名传递，不透传原始输入")
    void passesWhitelistedColumnName() {
        // Arrange & Act
        repository.selectBlueprintsInvtypeUniverse(BlueprintsPageCriteria.builder()
                .ownerId("2112832425")
                .sortField(BlueprintsPageCriteria.SortField.TYPE_NAME)
                .ascending(true)
                .build());

        // Assert
        verify(blueprintsMapper).selectBlueprintsInvtypeUniverse(
                any(), eq("2112832425"), any(), any(), eq("it.name"), eq(true));
    }

    @Test
    @DisplayName("未指定排序字段时列名传 null，由 SQL 使用默认排序")
    void passesNullColumnWhenUnsorted() {
        // Arrange & Act
        repository.selectBlueprintsInvtypeUniverse(BlueprintsPageCriteria.builder()
                .ownerId("2112832425")
                .build());

        // Assert
        verify(blueprintsMapper).selectBlueprintsInvtypeUniverse(
                any(), anyString(), any(), any(), eq((String) null), anyBoolean());
    }

    @Test
    @DisplayName("分页参数透传到 MyBatis Page")
    void passesPagination() {
        // Arrange & Act
        repository.selectBlueprintsInvtypeUniverse(BlueprintsPageCriteria.builder()
                .ownerId("2112832425")
                .current(3)
                .size(50)
                .build());

        // Assert
        ArgumentCaptor<IPage<BlueprintsDTO>> captor = ArgumentCaptor.forClass(IPage.class);
        verify(blueprintsMapper).selectBlueprintsInvtypeUniverse(
                captor.capture(), anyString(), any(), any(), any(), anyBoolean());
        assertEquals(3L, captor.getValue().getCurrent());
        assertEquals(50L, captor.getValue().getSize());
    }
}
