package xyz.foolcat.eve.evehelper.infrastructure.external.appws;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.port.appws.AppNotificationPushGateway;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * APP WebSocket 会话注册表。
 * <p>
 * 管理 APP 客户端的 WebSocket 会话生命周期，实现 {@link AppNotificationPushGateway} 领域端口。
 * <ul>
 *   <li>数据结构：{@code ConcurrentHashMap<Integer, CopyOnWriteArraySet<Session>>}</li>
 *   <li>unregister 使用 computeIfPresent 原子双清理（set 移除 session + set 空则移除 map 条目），
 *       规避 OneBot SESSION_POOL 泄漏先例</li>
 *   <li>pushToUser 每 session 独立 try-catch，一个失败不影响其他</li>
 *   <li>发送范式：{@code synchronized(session) { session.getAsyncRemote().sendText(payload); }}
 *       （仿 OneBot WebSocket.java:120-132）</li>
 * </ul>
 *
 * @author Leojian
 */
@Slf4j
@Component
public class AppWebSocketSessionRegistry implements AppNotificationPushGateway {

    private final ConcurrentHashMap<Integer, CopyOnWriteArraySet<Session>> sessions = new ConcurrentHashMap<>();

    /**
     * 注册会话。由 WebSocket 端点的 onOpen 调用。
     *
     * @param userId  用户 ID
     * @param session WebSocket 会话
     */
    public void register(Integer userId, Session session) {
        sessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("【APP WS】注册会话 userId={}, 当前会话数={}", userId, sessionSetSize(userId));
    }

    /**
     * 注销会话。由 WebSocket 端点的 onClose 调用。
     * <p>
     * 使用 computeIfPresent 原子双清理：移除 session 后若 set 为空则移除 map 条目，
     * 规避 OneBot SESSION_POOL 只 remove session 不清 map 的泄漏先例。
     *
     * @param userId  用户 ID
     * @param session WebSocket 会话
     */
    public void unregister(Integer userId, Session session) {
        sessions.computeIfPresent(userId, (k, set) -> {
            set.remove(session);
            return set.isEmpty() ? null : set;
        });
        log.debug("【APP WS】注销会话 userId={}", userId);
    }

    @Override
    public boolean isOnline(Integer userId) {
        CopyOnWriteArraySet<Session> set = sessions.get(userId);
        return set != null && !set.isEmpty();
    }

    @Override
    public void pushToUser(Integer userId, String payload) {
        CopyOnWriteArraySet<Session> set = sessions.get(userId);
        if (set == null) {
            return;
        }
        for (Session session : set) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                synchronized (session) {
                    session.getAsyncRemote().sendText(payload);
                }
            } catch (Exception e) {
                log.error("【APP WS】推送失败 userId={}, {}", userId, e.getMessage());
            }
        }
    }

    @Override
    public void closeUserSessions(Integer userId) {
        CopyOnWriteArraySet<Session> set = sessions.remove(userId);
        if (set == null) {
            return;
        }
        for (Session session : set) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("【APP WS】关闭会话异常 userId={}, {}", userId, e.getMessage());
            }
        }
        log.debug("【APP WS】关闭用户全部会话 userId={}, 会话数={}", userId, set.size());
    }

    // ---------- helper ----------

    private int sessionSetSize(Integer userId) {
        CopyOnWriteArraySet<Session> set = sessions.get(userId);
        return set == null ? 0 : set.size();
    }
}
