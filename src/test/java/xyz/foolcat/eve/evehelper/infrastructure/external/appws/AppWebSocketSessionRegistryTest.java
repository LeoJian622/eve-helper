package xyz.foolcat.eve.evehelper.infrastructure.external.appws;

import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * APP WebSocket 会话注册表单元测试。
 * <p>
 * 纯单元测试，mock Session，无需 DB/Spring context。
 *
 * @author Leojian
 */
@DisplayName("APP WebSocket 会话注册表测试")
class AppWebSocketSessionRegistryTest {

    private AppWebSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AppWebSocketSessionRegistry();
    }

    @Test
    @DisplayName("pushToUser 多设备场景：所有会话均收到推送")
    void pushToUser_multiDevice_fanoutAllSessions() throws Exception {
        Integer userId = 100;
        Session session1 = mockOpenSession();
        Session session2 = mockOpenSession();

        registry.register(userId, session1);
        registry.register(userId, session2);

        String payload = "{\"type\":\"test\",\"data\":\"hello\"}";
        registry.pushToUser(userId, payload);

        RemoteEndpoint.Async async1 = session1.getAsyncRemote();
        RemoteEndpoint.Async async2 = session2.getAsyncRemote();
        verify(async1, times(1)).sendText(payload);
        verify(async2, times(1)).sendText(payload);
    }

    @Test
    @DisplayName("pushToUser 离线用户：无操作无异常")
    void pushToUser_offlineUser_noOp() {
        Integer userId = 999;
        assertDoesNotThrow(() -> registry.pushToUser(userId, "{\"type\":\"test\"}"));
    }

    @Test
    @DisplayName("unregister 移除会话并清理空集合")
    void unregister_removesSession_andEmptySetCleaned() throws Exception {
        Integer userId = 200;
        Session session = mockOpenSession();

        registry.register(userId, session);
        assertTrue(registry.isOnline(userId));

        registry.unregister(userId, session);
        assertFalse(registry.isOnline(userId));

        // 验证空 set 清理后重新注册正常
        Session newSession = mockOpenSession();
        registry.register(userId, newSession);
        assertTrue(registry.isOnline(userId));
    }

    @Test
    @DisplayName("closeUserSessions 关闭所有会话并清除注册")
    void closeUserSessions_closesAllAndClears() throws Exception {
        Integer userId = 300;
        Session session1 = mockOpenSession();
        Session session2 = mockOpenSession();

        registry.register(userId, session1);
        registry.register(userId, session2);

        registry.closeUserSessions(userId);

        verify(session1, times(1)).close();
        verify(session2, times(1)).close();
        assertFalse(registry.isOnline(userId));
    }

    @Test
    @DisplayName("pushToUser 单会话失败不影响其他会话接收")
    void pushToUser_oneSessionFails_othersStillReceive() throws Exception {
        Integer userId = 400;
        Session session1 = mockOpenSession();
        Session session2 = mockOpenSession();

        // session1 的 async sendText 抛异常
        RemoteEndpoint.Async async1 = session1.getAsyncRemote();
        doThrow(new RuntimeException("boom")).when(async1).sendText(anyString());

        registry.register(userId, session1);
        registry.register(userId, session2);

        String payload = "{\"type\":\"test\"}";
        assertDoesNotThrow(() -> registry.pushToUser(userId, payload));

        // 两个 async 均被调用 sendText
        verify(async1, times(1)).sendText(payload);
        verify(session2.getAsyncRemote(), times(1)).sendText(payload);
    }

    // ---------- helper ----------

    private Session mockOpenSession() {
        Session session = mock(Session.class);
        when(session.isOpen()).thenReturn(true);
        RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(async);
        return session;
    }
}
