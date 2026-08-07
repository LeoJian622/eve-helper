package xyz.foolcat.eve.evehelper.infrastructure.external.onebot;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocket 入口鉴权单元测试(Mockito mockStatic,不启动 Spring 上下文)。
 * <p>
 * 覆盖 HIGH 2 修复:onOpen fail-closed 共享密钥鉴权。
 * 密钥未配置/为空/token 不匹配/token 缺失 一律拒绝并关闭连接;密钥正确且匹配才放行。
 *
 * @author Leojan
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket 入口鉴权测试 - fail-closed 共享密钥")
class WebSocketTest {

    private MockedStatic<SpringUtil> springMock;

    @Mock private Environment environment;
    @Mock private BotDispatcher botDispatcher;

    @BeforeEach
    void setUp() {
        // WebSocket 实例化与 onOpen 均通过 SpringUtil 静态取 Bean,需 mockStatic 桩定
        springMock = mockStatic(SpringUtil.class);
        springMock.when(() -> SpringUtil.getBean(Environment.class)).thenReturn(environment);
        springMock.when(() -> SpringUtil.getBean("botDispatcher")).thenReturn(botDispatcher);
    }

    @AfterEach
    void tearDown() {
        springMock.close();
    }

    /**
     * 清理 WebSocket 静态会话集合,隔离测试(避免跨用例状态泄漏)。
     */
    @AfterEach
    void clearStaticState() {
        try {
            Field sessions = WebSocket.class.getDeclaredField("SESSIONS");
            sessions.setAccessible(true);
            ((Collection<?>) sessions.get(null)).clear();
            Field pool = WebSocket.class.getDeclaredField("SESSION_POOL");
            pool.setAccessible(true);
            ((Map<?, ?>) pool.get(null)).clear();
        } catch (Exception ignored) {
            // 反射失败不阻塞测试
        }
    }

    @Test
    @DisplayName("密钥正确且 token 匹配,放行连接(不关闭 session)")
    void onOpen_secretCorrectTokenMatches_allowsConnection() throws Exception {
        when(environment.getProperty("onebot.websocket.secret")).thenReturn("correct-secret");
        Session session = mock(Session.class);
        when(session.getQueryString()).thenReturn("token=correct-secret");

        new WebSocket().onOpen(session, "user1");

        verify(session, never()).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("密钥未配置(secret 为空),fail-closed 拒绝连接")
    void onOpen_secretBlank_rejectsConnection() throws Exception {
        when(environment.getProperty("onebot.websocket.secret")).thenReturn("");
        Session session = mock(Session.class);
        when(session.getQueryString()).thenReturn("token=anything");

        new WebSocket().onOpen(session, "user1");

        verify(session).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("token 不匹配,fail-closed 拒绝连接")
    void onOpen_tokenMismatch_rejectsConnection() throws Exception {
        when(environment.getProperty("onebot.websocket.secret")).thenReturn("correct-secret");
        Session session = mock(Session.class);
        when(session.getQueryString()).thenReturn("token=wrong");

        new WebSocket().onOpen(session, "user1");

        verify(session).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("token 参数缺失,fail-closed 拒绝连接")
    void onOpen_tokenMissing_rejectsConnection() throws Exception {
        when(environment.getProperty("onebot.websocket.secret")).thenReturn("correct-secret");
        Session session = mock(Session.class);
        when(session.getQueryString()).thenReturn("");

        new WebSocket().onOpen(session, "user1");

        verify(session).close(any(CloseReason.class));
    }
}
