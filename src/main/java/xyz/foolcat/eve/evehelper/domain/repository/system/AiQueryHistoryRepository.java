package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.AiQueryHistory;

import java.util.List;

/**
 * AI查询历史仓储接口
 */
public interface AiQueryHistoryRepository {

    /**
     * 保存查询历史
     */
    AiQueryHistory save(AiQueryHistory history);

    /**
     * 根据用户ID查询历史
     *
     * @param userId 用户ID
     * @param limit  限制条数
     * @return 查询历史列表
     */
    List<AiQueryHistory> findByUserId(Integer userId, int limit);

    /**
     * 根据ID查询
     */
    AiQueryHistory findById(Long id);

    /**
     * 根据ID删除
     */
    void deleteById(Long id);
}
