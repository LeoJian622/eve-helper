package xyz.foolcat.eve.evehelper.domain.repository.system;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.List;

/**
 * APP 通知仓储接口（领域层）。
 * <p>
 * 定义通知通道的持久化操作契约，包括批量插入、幂等检查、分页查询、已读标记和过期清理。
 *
 * @author Leojian
 * date 2026-11-13
 */
public interface AppNotificationRepository {

    /**
     * 批量插入通知。
     *
     * @param notifications 通知列表（空或 null 返回 0，不调 DB）
     * @return 插入行数
     */
    int insertBatch(List<AppNotification> notifications);

    /**
     * 幂等键是否存在（无时间窗口，一次性事件永久去重）。
     *
     * @param dedupKey 幂等键
     * @return true=已存在
     */
    boolean existsByDedupKey(String dedupKey);

    /**
     * 幂等键在指定小时窗口内是否存在。
     *
     * @param dedupKey 幂等键
     * @param hours    回溯小时数
     * @return true=窗口内已存在
     */
    boolean existsByDedupKeyWithinHours(String dedupKey, int hours);

    /**
     * 按用户分页查询通知。
     *
     * @param userId     用户ID
     * @param unreadOnly true=仅未读；false/null=全部
     * @param pageQuery  分页参数
     * @return 分页结果
     */
    PageResult<AppNotification> pageByUser(Integer userId, Boolean unreadOnly, PageQuery pageQuery);

    /**
     * 统计用户未读通知数。
     *
     * @param userId 用户ID
     * @return 未读数
     */
    long countUnread(Integer userId);

    /**
     * 标记通知已读（归属内建 WHERE：WHERE id=? AND user_id=?）。
     *
     * @param id     通知ID
     * @param userId 用户ID
     * @return 受影响行数（他人行返回 0）
     */
    int markRead(Long id, Integer userId);

    /**
     * 分批删除过期通知。
     *
     * @param retentionDays 保留天数（早于 now-retentionDays 的行被删）
     * @param batchSize     每批删除上限（LIMIT）
     * @return 本次删除行数
     */
    int deleteExpired(int retentionDays, int batchSize);
}
