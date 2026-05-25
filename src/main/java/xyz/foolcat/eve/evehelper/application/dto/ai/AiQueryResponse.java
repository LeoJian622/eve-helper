package xyz.foolcat.eve.evehelper.application.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.foolcat.eve.evehelper.domain.service.ai.ResultFormatService;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * AI查询响应DTO
 */
@Data
@Schema(description = "AI查询响应")
public class AiQueryResponse {

    @Schema(description = "查询记录ID")
    private Long queryId;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "用户自然语言问题")
    private String userQuestion;

    @Schema(description = "AI生成的SQL语句")
    private String generatedSql;

    @Schema(description = "查询结果列表")
    private List<Map<String, Object>> results;

    @Schema(description = "结果数量")
    private int resultCount;

    @Schema(description = "执行耗时（毫秒）")
    private long executionTime;

    @Schema(description = "是否执行成功")
    private boolean success;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "查询时间")
    private Date timestamp;

    // ========== 可视化相关字段 ==========

    @Schema(description = "列元数据（类型、名称等）")
    private List<ResultFormatService.ColumnMeta> columns;

    @Schema(description = "图表建议列表")
    private List<ResultFormatService.ChartSuggestion> chartSuggestions;

    @Schema(description = "是否还有更多数据（超过50条）")
    private boolean hasMore;

    @Schema(description = "实际展示的数据条数")
    private int displayCount;
}
