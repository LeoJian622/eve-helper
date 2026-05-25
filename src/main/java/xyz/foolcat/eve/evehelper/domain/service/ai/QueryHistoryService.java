package xyz.foolcat.eve.evehelper.domain.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AiQueryHistory;
import xyz.foolcat.eve.evehelper.domain.repository.system.AiQueryHistoryRepository;

import java.util.List;

/**
 * 查询历史服务
 * 提供查询历史的CRUD操作和统计功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryHistoryService {

    private final AiQueryHistoryRepository historyRepository;

    /**
     * 获取用户的查询历史
     *
     * @param userId 用户ID
     * @param limit  限制条数
     * @return 查询历史列表
     */
    public List<AiQueryHistory> getUserHistory(Integer userId, int limit) {
        log.debug("Getting query history for user: {}, limit: {}", userId, limit);
        return historyRepository.findByUserId(userId, limit);
    }

    /**
     * 根据ID获取查询详情
     *
     * @param id 记录ID
     * @return 查询详情
     */
    public AiQueryHistory getHistoryById(Long id) {
        log.debug("Getting query history by id: {}", id);
        return historyRepository.findById(id);
    }

    /**
     * 删除查询历史记录
     *
     * @param id 记录ID
     */
    public void deleteHistory(Long id) {
        log.debug("Deleting query history: {}", id);
        historyRepository.deleteById(id);
    }

    /**
     * 保存查询历史
     *
     * @param history 查询历史
     * @return 保存后的历史记录
     */
    public AiQueryHistory saveHistory(AiQueryHistory history) {
        return historyRepository.save(history);
    }

    /**
     * 获取用户最近的成功查询记录
     * 用于上下文关联查询
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 查询历史列表
     */
    public List<AiQueryHistory> getRecentSuccessfulQueries(Integer userId, String sessionId, int limit) {
        log.debug("Getting recent successful queries for user: {}, session: {}", userId, sessionId);

        List<AiQueryHistory> allHistory = historyRepository.findByUserId(userId, limit * 2);

        return allHistory.stream()
                .filter(h -> Boolean.TRUE.equals(h.getSuccess()))
                .filter(h -> sessionId == null || sessionId.equals(h.getSessionId()))
                .limit(limit)
                .toList();
    }
}
