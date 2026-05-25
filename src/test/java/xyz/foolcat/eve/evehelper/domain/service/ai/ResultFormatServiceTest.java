package xyz.foolcat.eve.evehelper.domain.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.application.dto.ai.AiQueryResponse;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 结果格式化服务单元测试
 */
class ResultFormatServiceTest {

    private ResultFormatService formatService;

    @BeforeEach
    void setUp() {
        formatService = new ResultFormatService();
    }

    @Test
    @DisplayName("应该正确处理空响应")
    void shouldHandleNullResponse() {
        AiQueryResponse result = formatService.formatResults(null);
        assertNull(result);
    }

    @Test
    @DisplayName("应该正确处理失败的响应")
    void shouldHandleUnsuccessfulResponse() {
        AiQueryResponse response = new AiQueryResponse();
        response.setSuccess(false);

        AiQueryResponse result = formatService.formatResults(response);
        assertNull(result.getColumns());
        assertNull(result.getChartSuggestions());
    }

    @Test
    @DisplayName("应该正确处理空结果集")
    void shouldHandleEmptyResults() {
        AiQueryResponse response = new AiQueryResponse();
        response.setSuccess(true);
        response.setResults(Collections.emptyList());

        AiQueryResponse result = formatService.formatResults(response);
        assertNull(result.getColumns());
        assertNull(result.getChartSuggestions());
    }

    @Test
    @DisplayName("应该正确推断列类型")
    void shouldInferColumnTypes() {
        AiQueryResponse response = createTestResponseWithMixedTypes();

        AiQueryResponse result = formatService.formatResults(response);

        assertNotNull(result.getColumns());
        assertEquals(5, result.getColumns().size());

        // 验证类型推断
        Map<String, ResultFormatService.ColumnMeta> columnMap = new HashMap<>();
        result.getColumns().forEach(c -> columnMap.put(c.getName(), c));

        assertEquals(ResultFormatService.ColumnType.STRING, columnMap.get("name").getType());
        assertEquals(ResultFormatService.ColumnType.INTEGER, columnMap.get("age").getType());
        assertEquals(ResultFormatService.ColumnType.DECIMAL, columnMap.get("salary").getType());
        assertTrue(columnMap.get("age").isNumeric());
        assertTrue(columnMap.get("salary").isNumeric());
    }

    @Test
    @DisplayName("应该生成表格图表建议")
    void shouldGenerateTableChartSuggestion() {
        AiQueryResponse response = createTestResponseWithMixedTypes();

        AiQueryResponse result = formatService.formatResults(response);

        assertNotNull(result.getChartSuggestions());
        assertFalse(result.getChartSuggestions().isEmpty());

        // 表格建议应该总是存在
        assertTrue(result.getChartSuggestions().stream()
                .anyMatch(c -> c.getChartType() == ResultFormatService.ChartType.TABLE));
    }

    @Test
    @DisplayName("应该为数值数据生成柱状图建议")
    void shouldGenerateBarChartSuggestionForNumericData() {
        AiQueryResponse response = createTestResponseWithMixedTypes();

        AiQueryResponse result = formatService.formatResults(response);

        assertTrue(result.getChartSuggestions().stream()
                .anyMatch(c -> c.getChartType() == ResultFormatService.ChartType.BAR));
    }

    @Test
    @DisplayName("应该为时间序列数据生成折线图建议")
    void shouldGenerateLineChartSuggestionForTimeSeries() {
        AiQueryResponse response = createTestResponseWithDate();

        AiQueryResponse result = formatService.formatResults(response);

        assertTrue(result.getChartSuggestions().stream()
                .anyMatch(c -> c.getChartType() == ResultFormatService.ChartType.LINE));
    }

    @Test
    @DisplayName("应该为分类数据生成饼图建议")
    void shouldGeneratePieChartSuggestionForCategoricalData() {
        AiQueryResponse response = createTestResponseWithMixedTypes();

        AiQueryResponse result = formatService.formatResults(response);

        assertTrue(result.getChartSuggestions().stream()
                .anyMatch(c -> c.getChartType() == ResultFormatService.ChartType.PIE));
    }

    @Test
    @DisplayName("应该正确设置hasMore标志")
    void shouldSetHasMoreFlagCorrectly() {
        AiQueryResponse response = createTestResponseWithMixedTypes();
        response.setResultCount(60);

        AiQueryResponse result = formatService.formatResults(response);

        assertTrue(result.isHasMore());
        assertEquals(50, result.getDisplayCount());
    }

    @Test
    @DisplayName("结果少于50条时hasMore应该为false")
    void shouldSetHasMoreFalseForSmallResults() {
        AiQueryResponse response = createTestResponseWithMixedTypes();
        response.setResultCount(10);

        AiQueryResponse result = formatService.formatResults(response);

        assertFalse(result.isHasMore());
        assertEquals(10, result.getDisplayCount());
    }

    @Test
    @DisplayName("图表建议应该按置信度排序")
    void shouldSortChartSuggestionsByConfidence() {
        AiQueryResponse response = createTestResponseWithDate();

        AiQueryResponse result = formatService.formatResults(response);

        List<ResultFormatService.ChartSuggestion> suggestions = result.getChartSuggestions();
        for (int i = 1; i < suggestions.size(); i++) {
            assertTrue(suggestions.get(i - 1).getConfidence() >= suggestions.get(i).getConfidence());
        }
    }

    @Test
    @DisplayName("应该处理空值类型推断")
    void shouldHandleNullValuesForTypeInference() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("nullField", null);
        results.add(row);

        AiQueryResponse response = new AiQueryResponse();
        response.setSuccess(true);
        response.setResults(results);

        AiQueryResponse result = formatService.formatResults(response);

        assertNotNull(result.getColumns());
        assertEquals(ResultFormatService.ColumnType.UNKNOWN, result.getColumns().get(0).getType());
    }

    private AiQueryResponse createTestResponseWithMixedTypes() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "张三");
        row1.put("age", 30);
        row1.put("salary", new BigDecimal("15000.50"));
        row1.put("active", true);
        row1.put("department", "技术部");
        results.add(row1);

        AiQueryResponse response = new AiQueryResponse();
        response.setSuccess(true);
        response.setResults(results);
        response.setResultCount(results.size());
        return response;
    }

    private AiQueryResponse createTestResponseWithDate() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("date", new Date());
        row1.put("value", 100);
        results.add(row1);

        AiQueryResponse response = new AiQueryResponse();
        response.setSuccess(true);
        response.setResults(results);
        response.setResultCount(results.size());
        return response;
    }
}
