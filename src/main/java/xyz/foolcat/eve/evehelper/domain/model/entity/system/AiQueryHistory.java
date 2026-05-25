package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

/**
 * AI查询历史记录 - 领域实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiQueryHistory extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 用户自然语言问题
     */
    private String userQuestion;

    /**
     * AI生成的SQL语句
     */
    private String generatedSql;

    /**
     * 返回结果数量
     */
    private Integer resultCount;

    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;

    /**
     * 是否执行成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 会话ID
     */
    private String sessionId;

    public AiQueryHistory() {
        this.success = true;
        this.resultCount = 0;
        this.executionTime = 0L;
    }
}
