package xyz.foolcat.eve.evehelper.infrastructure.external.onebot;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.model.MessageEvent;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.security.MessageDigest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Webscoket 消息处理器
 *
 * @author Leojan
 * date 2024-06-21 11:54
 */

@Component
@Slf4j
@ServerEndpoint("/websocket/onebot/{userId}")
public class WebSocket {

    private final BotDispatcher botDispatcher = SpringUtil.getBean("botDispatcher");

    /**
     * 线程安全的无序的集合
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();

    /**
     * 存储在线连接数
     */
    private static final Map<String, Session> SESSION_POOL = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        // 共享密钥鉴权(fail-closed):napcat 须在连接 URL 携带 ?token=<secret>;密钥未配置或不匹配一律拒绝,
        // 不依赖 SecurityConfig 的 permitAll 兜底(与 AuthorizeUtil fail-closed 原则一致)。
        String secret = SpringUtil.getBean(Environment.class).getProperty("onebot.websocket.secret");
        String token = extractQueryParam(session.getQueryString(), "token");
        // 常量时间比较:抵御时序侧信道逐字节猜测 secret(String.equals 首字节不匹配即返回)。
        if (secret == null || secret.isBlank() || token == null
                || !MessageDigest.isEqual(
                        secret.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))) {
            log.warn("【WebSocket消息】连接鉴权失败,userId={},关闭连接", userId);
            try {
                session.close(new CloseReason(() -> 1008, "鉴权失败"));
            } catch (IOException e) {
                log.error("【WebSocket消息】关闭连接异常,{}", e.getMessage());
            }
            return;
        }
        try {
            SESSIONS.add(session);
            SESSION_POOL.put(userId, session);
            log.debug("【WebSocket消息】有新的连接，总数为：" + SESSIONS.size());
        } catch (Exception e) {
            log.error("【WebSocket消息】有新的连接异常,{}", e.getMessage());
        }
    }

    /**
     * 从查询字符串提取指定参数值(URL 解码)。
     */
    private String extractQueryParam(String queryString, String name) {
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }
        for (String pair : queryString.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && name.equals(pair.substring(0, idx))) {
                return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @OnClose
    public void onClose(Session session) {
        try {
            SESSIONS.remove(session);
            log.debug("【WebSocket消息】连接断开，总数为：" + SESSIONS.size());
        } catch (Exception e) {
            log.error("【WebSocket消息】连接断开异常,{}", e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("【WebSocket消息】收到客户端消息：" + message);
        MessageEvent messageEvent = JSONUtil.toBean(message, MessageEvent.class);
        JSONObject dispatchers = botDispatcher.dispatchers(messageEvent);
        if (dispatchers != null) {
                sendOneMessage("napcat", dispatchers.toJSONString(4));
        }
    }

    /**
     * 此为广播消息
     *
     * @param message 消息
     */
    public void sendAllMessage(String message) {
        log.debug("【WebSocket消息】广播消息：" + message);
        for (Session session : SESSIONS) {
            try {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                log.error("【WebSocket消息】广播消息异常,{}", e.getMessage());
            }
        }
    }

    /**
     * 此为单点消息
     *
     * @param userId  用户编号
     * @param message 消息
     */
    public void sendOneMessage(String userId, String message) {
        Session session = SESSION_POOL.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    log.debug("【WebSocket消息】单点消息：" + message);
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                log.error("【WebSocket消息】消息异常,{}", e.getMessage());
            }
        }
    }

    /**
     * 此为单点消息(多人)
     *
     * @param userIds 用户编号列表
     * @param message 消息
     */
    public void sendMoreMessage(String[] userIds, String message) {
        for (String userId : userIds) {
            Session session = SESSION_POOL.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    log.debug("【WebSocket消息】单点消息：" + message);
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    log.error("【WebSocket消息】消息异常,{}", e.getMessage());
                }
            }
        }
    }
}
