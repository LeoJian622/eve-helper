package xyz.foolcat.eve.evehelper.infrastructure.external.onebot;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.model.MessageEvent;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;
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
    private static final Map<String, Session> SESSION_POOL = new HashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        try {
            SESSIONS.add(session);
            SESSION_POOL.put(userId, session);
            log.debug("【WebSocket消息】有新的连接，总数为：" + SESSIONS.size());
        } catch (Exception e) {
            log.error("【WebSocket消息】有新的连接异常,{}", e.getMessage());
        }
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

