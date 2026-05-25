package xyz.foolcat.eve.evehelper.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

/**
 * AI查询历史记录 - 持久化对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("ai_query_history")
public class AiQueryHistoryPO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("user_question")
    private String userQuestion;

    @TableField("generated_sql")
    private String generatedSql;

    @TableField("result_count")
    private Integer resultCount;

    @TableField("execution_time")
    private Long executionTime;

    @TableField("success")
    private Boolean success;

    @TableField("error_message")
    private String errorMessage;

    @TableField("session_id")
    private String sessionId;

}
