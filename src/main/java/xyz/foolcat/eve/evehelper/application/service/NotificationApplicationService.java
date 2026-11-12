package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.dto.response.NotificationItem;
import xyz.foolcat.eve.evehelper.application.dto.response.UnreadCountResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.WsTicketResponse;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.AppNotificationRepository;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 通知应用服务(022 US4)。
 *
 * <p>协调仓储与缓存端口,提供分页查询、未读计数、已读标记和 WS 票据创建。
 * 每方法首行守卫 userId,fail-closed。</p>
 *
 * @author Leojian
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationApplicationService {

    private final AppNotificationRepository notificationRepository;
    private final CacheGateway cacheGateway;

    /**
     * 分页查询当前用户的通知。
     *
     * @param unreadOnly true=仅未读;false/null=全部
     * @param pageQuery  分页参数
     * @return 分页结果(records 已映射为 DTO)
     */
    public PageResult<NotificationItem> page(Boolean unreadOnly, PageQuery pageQuery) {
        Integer userId = requireAuthenticatedUserId();

        PageResult<AppNotification> repoResult =
                notificationRepository.pageByUser(userId, unreadOnly, pageQuery);

        List<NotificationItem> items = repoResult.getRecords().stream()
                .map(NotificationItem::from)
                .toList();

        return PageResult.<NotificationItem>builder()
                .records(items)
                .total(repoResult.getTotal())
                .current(repoResult.getCurrent())
                .size(repoResult.getSize())
                .pages(repoResult.getPages())
                .hasNext(repoResult.getHasNext())
                .hasPrevious(repoResult.getHasPrevious())
                .build();
    }

    /**
     * 统计当前用户的未读通知数。
     *
     * @return 未读数响应
     */
    public UnreadCountResponse unreadCount() {
        Integer userId = requireAuthenticatedUserId();
        return new UnreadCountResponse(notificationRepository.countUnread(userId));
    }

    /**
     * 标记通知已读(归属内建 WHERE)。
     * <p>
     * 他人行或不存在均返回 0 → 统一越权码,不泄露存在性。
     *
     * @param id 通知ID
     */
    public void markRead(Long id) {
        Integer userId = requireAuthenticatedUserId();
        int rows = notificationRepository.markRead(id, userId);
        if (rows == 0) {
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
    }

    /**
     * 创建 WebSocket 连接票据。
     * <p>
     * UUID 票据,30 秒 TTL 写入 Redis,键前缀 {@code app:ws:ticket:}。
     *
     * @return 票据响应
     */
    public WsTicketResponse createWsTicket() {
        Integer userId = requireAuthenticatedUserId();
        String ticket = UUID.randomUUID().toString();
        cacheGateway.setIfAbsent("app:ws:ticket:" + ticket, userId, 30, TimeUnit.SECONDS);
        return new WsTicketResponse(ticket, 30);
    }

    /**
     * 守卫:要求已认证用户 ID,fail-closed。
     *
     * @return 已认证用户 ID(> 0)
     * @throws EveHelperException 未认证或 ID 无效时抛出统一越权异常
     */
    private Integer requireAuthenticatedUserId() {
        Integer userId = UserUtil.getUserId();
        if (userId == null || userId <= 0) {
            throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
        }
        return userId;
    }
}
