package xyz.foolcat.eve.evehelper.domain.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.dto.ai.AiQueryResponse;

import java.math.BigDecimal;
import java.util.*;

/**
 * 查询结果格式化服务
 * 提供结果结构化、列类型推断、图表建议等功能
 */
@Slf4j
@Service
public class ResultFormatService {

    /**
     * 列类型枚举
     */
    public enum ColumnType {
        STRING,
        INTEGER,
        DECIMAL,
        DATE,
        DATETIME,
        BOOLEAN,
        UNKNOWN
    }

    /**
     * 图表类型枚举
     */
    public enum ChartType {
        TABLE("表格", "默认表格展示"),
        BAR("柱状图", "适合对比不同类别的数值"),
        LINE("折线图", "适合展示时间序列或趋势"),
        PIE("饼图", "适合展示占比分布"),
        SCATTER("散点图", "适合展示两个变量的相关性");

        private final String name;
        private final String description;

        ChartType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 列元数据
     */
    public static class ColumnMeta {
        private String name;
        private ColumnType type;
        private String typeName;
        private boolean numeric;
        private boolean dateType;

        public ColumnMeta(String name, ColumnType type) {
            this.name = name;
            this.type = type;
            this.typeName = type.name();
            this.numeric = type == ColumnType.INTEGER || type == ColumnType.DECIMAL;
            this.dateType = type == ColumnType.DATE || type == ColumnType.DATETIME;
        }

        public String getName() { return name; }
        public ColumnType getType() { return type; }
        public String getTypeName() { return typeName; }
        public boolean isNumeric() { return numeric; }
        public boolean isDateType() { return dateType; }
    }

    /**
     * 图表建议
     */
    public static class ChartSuggestion {
        private ChartType chartType;
        private String chartTypeName;
        private String description;
        private List<String> suggestedColumns;
        private int confidence;

        public ChartSuggestion(ChartType chartType, List<String> suggestedColumns, int confidence) {
            this.chartType = chartType;
            this.chartTypeName = chartType.getName();
            this.description = chartType.getDescription();
            this.suggestedColumns = suggestedColumns;
            this.confidence = confidence;
        }

        public ChartType getChartType() { return chartType; }
        public String getChartTypeName() { return chartTypeName; }
        public String getDescription() { return description; }
        public List<String> getSuggestedColumns() { return suggestedColumns; }
        public int getConfidence() { return confidence; }
    }

    /**
     * 格式化查询结果
     *
     * @param response 查询响应
     * @return 格式化后的响应
     */
    public AiQueryResponse formatResults(AiQueryResponse response) {
        if (response == null || !response.isSuccess() || response.getResults() == null || response.getResults().isEmpty()) {
            return response;
        }

        List<Map<String, Object>> results = response.getResults();
        Map<String, Object> firstRow = results.get(0);

        // 推断列类型
        List<ColumnMeta> columns = inferColumnTypes(firstRow);
        response.setColumns(columns);

        // 生成图表建议
        List<ChartSuggestion> chartSuggestions = generateChartSuggestions(columns, results);
        response.setChartSuggestions(chartSuggestions);

        // 添加数据预览信息
        response.setHasMore(response.getResultCount() > 50);
        response.setDisplayCount(Math.min(response.getResultCount(), 50));

        log.debug("Formatted query results: {} columns, {} chart suggestions",
                columns.size(), chartSuggestions.size());

        return response;
    }

    /**
     * 推断列类型
     */
    private List<ColumnMeta> inferColumnTypes(Map<String, Object> firstRow) {
        List<ColumnMeta> columns = new ArrayList<>();

        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();
            ColumnType type = inferType(value);
            columns.add(new ColumnMeta(columnName, type));
        }

        return columns;
    }

    /**
     * 根据值推断类型
     */
    private ColumnType inferType(Object value) {
        if (value == null) {
            return ColumnType.UNKNOWN;
        }

        if (value instanceof Integer || value instanceof Long || value instanceof Short) {
            return ColumnType.INTEGER;
        }

        if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
            return ColumnType.DECIMAL;
        }

        if (value instanceof java.sql.Timestamp || value instanceof java.sql.Timestamp) {
            return ColumnType.DATETIME;
        }

        if (value instanceof java.sql.Date || value instanceof Date) {
            return ColumnType.DATE;
        }

        if (value instanceof Boolean) {
            return ColumnType.BOOLEAN;
        }

        return ColumnType.STRING;
    }

    /**
     * 生成图表建议
     */
    private List<ChartSuggestion> generateChartSuggestions(List<ColumnMeta> columns, List<Map<String, Object>> results) {
        List<ChartSuggestion> suggestions = new ArrayList<>();

        // 表格总是建议
        suggestions.add(new ChartSuggestion(ChartType.TABLE,
                columns.stream().map(ColumnMeta::getName).toList(), 100));

        List<String> numericColumns = columns.stream()
                .filter(ColumnMeta::isNumeric)
                .map(ColumnMeta::getName)
                .toList();

        List<String> dateColumns = columns.stream()
                .filter(ColumnMeta::isDateType)
                .map(ColumnMeta::getName)
                .toList();

        List<String> stringColumns = columns.stream()
                .filter(c -> c.getType() == ColumnType.STRING)
                .map(ColumnMeta::getName)
                .toList();

        // 有数值列时建议柱状图
        if (!numericColumns.isEmpty()) {
            int confidence = numericColumns.size() >= 2 ? 80 : 60;
            List<String> suggestedCols = new ArrayList<>();
            if (!stringColumns.isEmpty()) {
                suggestedCols.add(stringColumns.get(0));
            }
            suggestedCols.addAll(numericColumns);
            suggestions.add(new ChartSuggestion(ChartType.BAR, suggestedCols, confidence));
        }

        // 有日期列和数值列时建议折线图
        if (!dateColumns.isEmpty() && !numericColumns.isEmpty()) {
            List<String> suggestedCols = new ArrayList<>();
            suggestedCols.add(dateColumns.get(0));
            suggestedCols.addAll(numericColumns);
            suggestions.add(new ChartSuggestion(ChartType.LINE, suggestedCols, 85));
        }

        // 有字符串列和至少一个数值列时建议饼图
        if (!stringColumns.isEmpty() && !numericColumns.isEmpty() && results.size() <= 20) {
            List<String> suggestedCols = new ArrayList<>();
            suggestedCols.add(stringColumns.get(0));
            suggestedCols.add(numericColumns.get(0));
            suggestions.add(new ChartSuggestion(ChartType.PIE, suggestedCols, 70));
        }

        // 至少有两个数值列时建议散点图
        if (numericColumns.size() >= 2 && results.size() >= 10) {
            suggestions.add(new ChartSuggestion(ChartType.SCATTER,
                    numericColumns.subList(0, Math.min(2, numericColumns.size())), 65));
        }

        // 按置信度排序
        suggestions.sort((a, b) -> Integer.compare(b.getConfidence(), a.getConfidence()));

        return suggestions;
    }
}
