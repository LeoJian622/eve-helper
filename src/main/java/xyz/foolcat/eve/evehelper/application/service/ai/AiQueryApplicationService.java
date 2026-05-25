package xyz.foolcat.eve.evehelper.application.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.dto.ai.AiQueryResponse;
import xyz.foolcat.eve.evehelper.application.dto.ai.QueryHistoryResponse;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AiQueryHistory;
import xyz.foolcat.eve.evehelper.domain.repository.system.AiQueryHistoryRepository;
import xyz.foolcat.eve.evehelper.domain.service.ai.QueryExecutionService;
import xyz.foolcat.eve.evehelper.domain.service.ai.ResultFormatService;
import xyz.foolcat.eve.evehelper.domain.service.ai.SqlGenerationService;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI查询应用服务
 * 协调领域服务完成查询流程
 * 使用 Spring AI ChatMemory 管理多轮对话记忆
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQueryApplicationService {

    private final SqlGenerationService sqlGenerationService;
    private final QueryExecutionService queryExecutionService;
    private final AiQueryHistoryRepository queryHistoryRepository;
    private final ResultFormatService resultFormatService;

    /**
     * 执行AI查询
     *
     * @param userId    用户ID
     * @param question  用户问题
     * @param sessionId 会话ID（可选）
     * @return 查询结果
     */
    @Transactional
    public AiQueryResponse executeQuery(Integer userId, String question, String sessionId) {
        long totalStartTime = System.currentTimeMillis();

        // 生成新的会话ID
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        AiQueryResponse response = new AiQueryResponse();
        response.setSessionId(sessionId);
        response.setUserQuestion(question);
        response.setTimestamp(new Date());

        // 1. 生成SQL（Spring AI ChatMemory自动管理多轮对话上下文）
        SqlGenerationService.GenerationResult generationResult =
                sqlGenerationService.generateAndValidate(question, sessionId);

        if (!generationResult.isSuccess()) {
            response.setSuccess(false);
            response.setErrorMessage(generationResult.getErrorMessage());
            response.setExecutionTime(System.currentTimeMillis() - totalStartTime);

            // 保存失败记录
            saveHistory(userId, question, null, 0, response.getExecutionTime(),
                    false, generationResult.getErrorMessage(), sessionId);

            return response;
        }

        String generatedSql = generationResult.getSql();
        response.setGeneratedSql(generatedSql);

        // 2. 执行SQL
        QueryExecutionService.ExecutionResult executionResult = queryExecutionService.executeQuery(generatedSql);

        long totalTime = System.currentTimeMillis() - totalStartTime;
        response.setExecutionTime(totalTime);
        response.setSuccess(executionResult.isSuccess());

        if (executionResult.isSuccess()) {
            response.setResults(executionResult.getResults());
            response.setResultCount(executionResult.getResultCount());
        } else {
            response.setErrorMessage(executionResult.getErrorMessage());
        }

        // 3. 保存查询历史（持久化历史记录）
        AiQueryHistory history = saveHistory(userId, question, generatedSql,
                response.getResultCount(), response.getExecutionTime(),
                response.isSuccess(), response.getErrorMessage(), sessionId);

        response.setQueryId(history.getId());

        // 4. 格式化结果（添加列类型信息和图表建议）
        resultFormatService.formatResults(response);

        log.info("AI query completed - sessionId: {}, success: {}, resultCount: {}, executionTime: {}ms",
                sessionId, response.isSuccess(), response.getResultCount(), response.getExecutionTime());

        return response;
    }

    /**
     * 清空会话记忆
     *
     * @param sessionId 会话ID
     */
    public void clearSessionMemory(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            sqlGenerationService.clearSessionMemory(sessionId);
            log.info("Cleared chat memory for session: {}", sessionId);
        }
    }

    /**
     * 获取会话历史消息数
     *
     * @param sessionId 会话ID
     * @return 历史消息数
     */
    public int getSessionHistoryCount(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return 0;
        }
        return sqlGenerationService.getSessionHistoryCount(sessionId);
    }

    /**
     * 获取用户的查询历史
     *
     * @param userId 用户ID
     * @param limit  限制条数
     * @return 查询历史列表
     */
    public List<QueryHistoryResponse> getQueryHistory(Integer userId, int limit) {
        List<AiQueryHistory> historyList = queryHistoryRepository.findByUserId(userId, limit);

        return historyList.stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取单条查询历史详情
     *
     * @param id 记录ID
     * @return 查询历史详情
     */
    public AiQueryResponse getQueryDetail(Long id) {
        AiQueryHistory history = queryHistoryRepository.findById(id);
        if (history == null) {
            return null;
        }

        AiQueryResponse response = new AiQueryResponse();
        response.setQueryId(history.getId());
        response.setSessionId(history.getSessionId());
        response.setUserQuestion(history.getUserQuestion());
        response.setGeneratedSql(history.getGeneratedSql());
        response.setResultCount(history.getResultCount());
        response.setExecutionTime(history.getExecutionTime());
        response.setSuccess(history.getSuccess());
        response.setErrorMessage(history.getErrorMessage());
        response.setTimestamp(history.getGmtCreate());

        return response;
    }

    /**
     * 删除查询历史记录
     *
     * @param id 记录ID
     */
    public void deleteQueryHistory(Long id) {
        queryHistoryRepository.deleteById(id);
    }

    private AiQueryHistory saveHistory(Integer userId, String question, String generatedSql,
                                       Integer resultCount, Long executionTime, Boolean success,
                                       String errorMessage, String sessionId) {
        AiQueryHistory history = new AiQueryHistory();
        history.setUserId(userId);
        history.setUserQuestion(question);
        history.setGeneratedSql(generatedSql != null ? generatedSql : "");
        history.setResultCount(resultCount);
        history.setExecutionTime(executionTime);
        history.setSuccess(success);
        history.setErrorMessage(errorMessage);
        history.setSessionId(sessionId);

        return queryHistoryRepository.save(history);
    }

    private QueryHistoryResponse convertToHistoryResponse(AiQueryHistory history) {
        QueryHistoryResponse response = new QueryHistoryResponse();
        response.setId(history.getId());
        response.setUserQuestion(history.getUserQuestion());
        response.setGeneratedSql(history.getGeneratedSql());
        response.setResultCount(history.getResultCount() != null ? history.getResultCount() : 0);
        response.setExecutionTime(history.getExecutionTime() != null ? history.getExecutionTime() : 0L);
        response.setSuccess(history.getSuccess() != null ? history.getSuccess() : false);
        response.setGmtCreate(history.getGmtCreate());
        return response;
    }
}
