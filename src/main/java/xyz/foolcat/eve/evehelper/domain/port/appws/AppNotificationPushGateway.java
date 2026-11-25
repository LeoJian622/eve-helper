package xyz.foolcat.eve.evehelper.domain.port.appws;

/**
 * APP 通知推送领域端口。
 * <p>
 * 领域层仅依赖本接口向 APP 客户端推送 WebSocket 消息，
 * 不直接操作 jakarta.websocket.Session。
 * 由基础设施层 {@code infrastructure/external/appws/AppWebSocketSessionRegistry} 实现。
 *
 * @author Leojian
 */
public interface AppNotificationPushGateway {

    /**
     * 判断指定用户是否在线（存在活跃 WebSocket 会话）。
     *
     * @param userId 用户 ID
     * @return true 表示在线
     */
    boolean isOnline(Integer userId);

    /**
     * 向指定用户的所有活跃会话推送消息。
     * <p>
     * 离线用户（无会话）视为 no-op，不抛异常。
     * 单个会话发送失败不影响其他会话。
     *
     * @param userId  用户 ID
     * @param payload JSON 信封字符串
     */
    void pushToUser(Integer userId, String payload);

    /**
     * 关闭指定用户的所有 WebSocket 会话并清除注册。
     *
     * @param userId 用户 ID
     */
    void closeUserSessions(Integer userId);
}
