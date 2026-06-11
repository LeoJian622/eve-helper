package xyz.foolcat.eve.evehelper.domain.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.infrastructure.config.ai.AiQueryProperties;

/**
 * SQL生成服务
 * 使用 Spring AI Chat Memory 管理多轮对话记忆
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlGenerationService {

    private final SpringAiChatService springAiChatService;
    private final DatabaseSchemaService schemaService;
    private final AiQueryProperties properties;

    /**
     * 生成并校验SQL（支持会话上下文）
     *
     * @param userQuestion 用户自然语言问题
     * @param sessionId    会话ID（用于记忆管理）
     * @return 生成的SQL
     */
    public GenerationResult generateAndValidate(String userQuestion, String sessionId) {
        try {
            // 获取数据库 Schema 上下文
            String schemaContext = schemaService.getSchemaContext();

            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(schemaContext);

            // 调用AI生成SQL（ChatMemory自动管理会话上下文）
            String generatedSql = springAiChatService.chat(sessionId, systemPrompt, userQuestion);
            System.out.println("generatedSql = " + generatedSql);
            generatedSql = StringUtils.substringBetween(generatedSql,"[SQL]","[/SQL]");

            if (generatedSql == null || generatedSql.trim().isEmpty()) {
                return GenerationResult.fail("AI生成的SQL为空");
            }

            // 检查是否是错误响应
            if (generatedSql.startsWith("ERROR:")) {
                return GenerationResult.fail(generatedSql);
            }

            // 安全校验
            SqlSecurityValidator.ValidationResult validationResult =
                    new SqlSecurityValidator().validate(generatedSql);
            if (!validationResult.isValid()) {
                return GenerationResult.fail("SQL安全校验失败: " + validationResult.getErrorMessage());
            }

            // 确保查询结果限制
            String finalSql = new SqlSecurityValidator().ensureLimit(
                    generatedSql, properties.getMaxResultLimit()
            );

            return GenerationResult.success(finalSql);

        } catch (Exception e) {
            log.error("SQL generation failed: {}", e.getMessage());
            return GenerationResult.fail("SQL生成失败: " + e.getMessage());
        }
    }

    /**
     * 清空会话记忆
     *
     * @param sessionId 会话ID
     */
    public void clearSessionMemory(String sessionId) {
        springAiChatService.clearMemory(sessionId);
    }

    /**
     * 获取会话历史消息数
     *
     * @param sessionId 会话ID
     * @return 历史消息数
     */
    public int getSessionHistoryCount(String sessionId) {
        return springAiChatService.getHistoryMessageCount(sessionId);
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String schemaContext) {
        return """
                你是一个专业的MySQL数据库查询专家。请根据用户的自然语言问题生成正确的SQL查询语句。
                
                要求：
                1. 请将生成的SQL语句包裹在 [SQL] 和 [/SQL] 标签之间，标签之间只有SQL语句。
                2. 只允许SELECT查询语句，禁止任何修改操作
                3. SQL语法必须符合MySQL标准
                4. 查询结果限制在50条以内（建议自动添加 LIMIT 50）
                5. 使用正确的表名和字段名
                6. 如果需要JOIN，请确保关联条件正确
                7. 确保WHERE条件中的类型匹配正确
                8. 使用范围查询

                数据库Schema信息如下：
                %s
                
                名词解释：
                击杀海盗赏金：bounty_prizes
                
                示例：
                用户：查询所有用户ID
                输出：[SQL]SELECT user_id FROM ai_query_history LIMIT 50;[/SQL] 
                
                用户：查询 EVE 账号表
                输出：[SQL]SELECT * FROM eve_account LIMIT 50;[/SQL] 

                如果无法理解用户的问题或无法生成正确的SQL，请返回：ERROR: 无法理解您的查询
                """.formatted(schemaContext);
    }

    /**
     * 生成结果
     */
    public static class GenerationResult {
        private final boolean success;
        private final String sql;
        private final String errorMessage;

        private GenerationResult(boolean success, String sql, String errorMessage) {
            this.success = success;
            this.sql = sql;
            this.errorMessage = errorMessage;
        }

        public static GenerationResult success(String sql) {
            return new GenerationResult(true, sql, null);
        }

        public static GenerationResult fail(String errorMessage) {
            return new GenerationResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getSql() {
            return sql;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
