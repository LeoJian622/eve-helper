package xyz.foolcat.eve.evehelper.infrastructure.external.appws;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * APP WebSocket 端点单元测试。
 * <p>
 * 纯单元测试，mock Session/CacheGateway/Registry + mockStatic SpringUtil，
 * 无需 DB/Spring context。
 *
 * @author Leojian
 */
@DisplayName("APP WebSocket 端点鉴权测试")
class AppWebSocketHandlerTest {

    private Session session;
    private CacheGateway cache;
    private AppWebSocketSessionRegistry registry;
    private AppNotificationWebSocket handler;

    @BeforeEach
    void setUp() {
        session = mock(Session.class);
        cache = mock(CacheGateway.class);
        registry = mock(AppWebSocketSessionRegistry.class);
        handler = new AppNotificationWebSocket();
    }

    @Test
    @DisplayName("onOpen 有效 ticket - 注册会话并消费 ticket")
    void onOpen_validTicket_registersAndConsumes() throws Exception {
        // Arrange
        Map<String, List<String>> params = Map.of("ticket", List.of("t-uuid"));
        when(session.getRequestParameterMap()).thenReturn(params);
        when(session.getUserProperties()).thenReturn(new HashMap<>());

        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(CacheGateway.class)).thenReturn(cache);
            springUtil.when(() -> SpringUtil.getBean(AppWebSocketSessionRegistry.class)).thenReturn(registry);

            when(cache.get("app:ws:ticket:t-uuid")).thenReturn(42);
            when(cache.compareAndDelete("app:ws:ticket:t-uuid", 42)).thenReturn(true);

            // Act
            handler.onOpen(session);

            // Assert
            verify(registry, times(1)).register(42, session);
            verify(session, times(1)).setMaxIdleTimeout(90000);
            verify(cache, times(1)).compareAndDelete("app:ws:ticket:t-uuid", 42);
            verify(session, never()).close(any(CloseReason.class));
        }
    }

    @Test
    @DisplayName("onOpen 缺失 ticket - 关闭连接")
    void onOpen_missingTicket_closes() throws Exception {
        // Arrange
        Map<String, List<String>> params = Map.of();
        when(session.getRequestParameterMap()).thenReturn(params);

        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(CacheGateway.class)).thenReturn(cache);
            springUtil.when(() -> SpringUtil.getBean(AppWebSocketSessionRegistry.class)).thenReturn(registry);

            // Act
            handler.onOpen(session);

            // Assert
            verify(session, times(1)).close(any(CloseReason.class));
            verify(registry, never()).register(any(), any());
            verify(cache, never()).get(anyString());
        }
    }

    @Test
    @DisplayName("onOpen 过期 ticket - 关闭连接")
    void onOpen_expiredTicket_closes() throws Exception {
        // Arrange
        Map<String, List<String>> params = Map.of("ticket", List.of("t-expired"));
        when(session.getRequestParameterMap()).thenReturn(params);

        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(CacheGateway.class)).thenReturn(cache);
            springUtil.when(() -> SpringUtil.getBean(AppWebSocketSessionRegistry.class)).thenReturn(registry);

            when(cache.get("app:ws:ticket:t-expired")).thenReturn(null);

            // Act
            handler.onOpen(session);

            // Assert
            verify(session, times(1)).close(any(CloseReason.class));
            verify(registry, never()).register(any(), any());
            verify(cache, never()).compareAndDelete(anyString(), any());
        }
    }

    @Test
    @DisplayName("onOpen 重放 ticket - 关闭连接")
    void onOpen_replayedTicket_closes() throws Exception {
        // Arrange
        Map<String, List<String>> params = Map.of("ticket", List.of("t-replay"));
        when(session.getRequestParameterMap()).thenReturn(params);

        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(CacheGateway.class)).thenReturn(cache);
            springUtil.when(() -> SpringUtil.getBean(AppWebSocketSessionRegistry.class)).thenReturn(registry);

            when(cache.get("app:ws:ticket:t-replay")).thenReturn(42);
            when(cache.compareAndDelete("app:ws:ticket:t-replay", 42)).thenReturn(false);

            // Act
            handler.onOpen(session);

            // Assert
            verify(session, times(1)).close(any(CloseReason.class));
            verify(registry, never()).register(any(), any());
        }
    }

    @Test
    @DisplayName("onClose 注销会话")
    void onClose_unregisters() {
        // Arrange
        HashMap<String, Object> userProps = new HashMap<>();
        userProps.put("appUserId", 42);
        when(session.getUserProperties()).thenReturn(userProps);

        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(AppWebSocketSessionRegistry.class)).thenReturn(registry);

            // Act
            handler.onClose(session);

            // Assert
            verify(registry, times(1)).unregister(42, session);
        }
    }
}
