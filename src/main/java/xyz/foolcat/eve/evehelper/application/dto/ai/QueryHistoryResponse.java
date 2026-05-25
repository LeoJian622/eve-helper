package xyz.foolcat.eve.evehelper.application.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.util.Date;

/**
 * 查询历史响应DTO
 */
@Data
@Schema(description = "查询历史记录")
public class QueryHistoryResponse extends BaseEntity {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "用户问题")
    private String userQuestion;

    @Schema(description = "生成的SQL")
    private String generatedSql;

    @Schema(description = "结果数量")
    private int resultCount;

    @Schema(description = "执行耗时（毫秒）")
    private long executionTime;

    @Schema(description = "是否成功")
    private boolean success;

}
