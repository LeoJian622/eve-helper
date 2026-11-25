package xyz.foolcat.eve.evehelper.infrastructure.external.appws;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * APP 通知通道 WebSocket 端点。
 * <p>
 * 一次性 ticket 鉴权：T027 REST 创建 ticket（键 {@code app:ws:ticket:{uuid}}，值 userId，TTL 30s），
 * 本端点 onOpen 消费 ticket（compareAndDelete 原子），成功后注册会话。
 * <p>
 * 安全裁定（brief 逐字）：
 * <ul>
 *   <li>先消费后注册（compareAndDelete → register）</li>
 *   <li>userId 存 session.getUserProperties()（单例共享，不用实例字段）</li>
 *   <li>JWT 不进 URL（端点只读 ticket 参数）</li>
 *   <li>ticket 值不入日志（凭证保护）</li>
 *   <li>fail-closed：val 非 Integer 一律关闭</li>
 * </ul>
 *
 * @author Leojian
 */
@Component
@Slf4j
@ServerEndpoint("/websocket/app")
public class AppNotificationWebSocket {

    private static final String TICKET_KEY_PREFIX = "app:ws:ticket:";
    private static final String USER_ID_PROP = "appUserId";
    private static final int AUTH_FAIL_CODE = 1008;
    private static final String AUTH_FAIL_REASON = "鉴权失败";
    private static final long MAX_IDLE_TIMEOUT_MS = 90000;

    @OnOpen
    public void onOpen(Session session) {
        CacheGateway cache = SpringUtil.getBean(CacheGateway.class);
        AppWebSocketSessionRegistry registry = SpringUtil.getBean(AppWebSocketSessionRegistry.class);

        // ① ticket 提取
        Map<String, List<String>> params = session.getRequestParameterMap();
        List<String> ticketList = params.get("ticket");
        if (ticketList == null || ticketList.isEmpty()) {
            log.warn("【APP WS】连接鉴权失败: ticket 缺失");
            closeAuthFail(session);
            return;
        }
        String ticket = ticketList.get(0);
        if (ticket == null || ticket.isEmpty()) {
            log.warn("【APP WS】连接鉴权失败: ticket 为空");
            closeAuthFail(session);
            return;
        }

        String key = TICKET_KEY_PREFIX + ticket;

        // ② cache.get → null 即过期/不存在
        Object val = cache.get(key);
        if (val == null) {
            log.warn("【APP WS】连接鉴权失败: ticket 过期或不存在");
            closeAuthFail(session);
            return;
        }

        // ③ val 非 Integer → fail-closed
        if (!(val instanceof Integer)) {
            log.warn("【APP WS】连接鉴权失败: ticket 值类型异常");
            closeAuthFail(session);
            return;
        }
        Integer userId = (Integer) val;

        // ④ compareAndDelete（原子消费）→ false 即重放
        Boolean ok = cache.compareAndDelete(key, userId);
        if (!Boolean.TRUE.equals(ok)) {
            log.warn("【APP WS】连接鉴权失败: ticket 重放或已被消费");
            closeAuthFail(session);
            return;
        }

        // ⑤ 成功：先消费后注册
        session.getUserProperties().put(USER_ID_PROP, userId);
        registry.register(userId, session);
        session.setMaxIdleTimeout(MAX_IDLE_TIMEOUT_MS);
        log.debug("【APP WS】连接鉴权成功 userId={}", userId);
    }

    @OnClose
    public void onClose(Session session) {
        Object userIdObj = session.getUserProperties().get(USER_ID_PROP);
        if (userIdObj == null) {
            return;
        }
        AppWebSocketSessionRegistry registry = SpringUtil.getBean(AppWebSocketSessionRegistry.class);
        registry.unregister((Integer) userIdObj, session);
    }

    private void closeAuthFail(Session session) {
        try {
            session.close(new CloseReason(() -> AUTH_FAIL_CODE, AUTH_FAIL_REASON));
        } catch (IOException e) {
            log.error("【APP WS】关闭连接异常: {}", e.getMessage());
        }
    }
}
