package xyz.foolcat.eve.evehelper.domain.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * SQL执行服务
 * 执行SQL查询并返回结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExecutionService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 执行SQL查询
     *
     * @param sql SQL语句
     * @return 查询结果
     */
    public ExecutionResult executeQuery(String sql) {
        log.info("Executing SQL: {}", sql);

        long startTime = System.currentTimeMillis();

        try {
            List<Map<String, Object>> results = jdbcTemplate.query(sql, new ColumnMapRowMapper());

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Query executed successfully, returned {} rows in {}ms", results.size(), executionTime);

            return ExecutionResult.success(results, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Query execution failed: {}", e.getMessage());
            return ExecutionResult.fail("SQL执行失败: " + e.getMessage(), executionTime);
        }
    }

    /**
     * 执行结果
     */
    public static class ExecutionResult {
        private final boolean success;
        private final List<Map<String, Object>> results;
        private final int resultCount;
        private final long executionTime;
        private final String errorMessage;

        private ExecutionResult(boolean success, List<Map<String, Object>> results,
                                long executionTime, String errorMessage) {
            this.success = success;
            this.results = results;
            this.resultCount = results != null ? results.size() : 0;
            this.executionTime = executionTime;
            this.errorMessage = errorMessage;
        }

        public static ExecutionResult success(List<Map<String, Object>> results, long executionTime) {
            return new ExecutionResult(true, results, executionTime, null);
        }

        public static ExecutionResult fail(String errorMessage, long executionTime) {
            return new ExecutionResult(false, null, executionTime, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public List<Map<String, Object>> getResults() {
            return results;
        }

        public int getResultCount() {
            return resultCount;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
