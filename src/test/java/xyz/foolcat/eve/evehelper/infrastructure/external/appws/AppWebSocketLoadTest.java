package xyz.foolcat.eve.evehelper.infrastructure.external.appws;

import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * T035 — 300 mock Session 扇出负载测试（SC-003/SC-008）。
 * <p>
 * 纯单元，直接 new AppWebSocketSessionRegistry()，无需 Spring context。
 * 验证 300 用户各 1 session 注册→单事件推送→全部收到 + p95 ≤ 5s + 无连接降级 + 原子双清理。
 *
 * @author Leojian
 */
@DisplayName("T035 APP WebSocket 300 会话扇出负载测试")
class AppWebSocketLoadTest {

    private static final int SESSION_COUNT = 300;
    private static final int BASE_USER_ID = 1000;
    private static final String PAYLOAD = "{\"type\":\"NOTIFICATION\",\"data\":{\"id\":1}}";
    private static final long P95_LIMIT_NS = 5_000_000_000L; // 5s

    private final AppWebSocketSessionRegistry registry = new AppWebSocketSessionRegistry();
    private final List<Integer> userIds = new ArrayList<>(SESSION_COUNT);
    private final List<Session> sessions = new ArrayList<>(SESSION_COUNT);
    private final List<RemoteEndpoint.Async> asyncRemotes = new ArrayList<>(SESSION_COUNT);

    @AfterEach
    void tearDown() {
        // 逐用户清理注册（防实例级污染；虽每用例 new，但显式清理更稳健）
        for (int i = 0; i < userIds.size(); i++) {
            registry.unregister(userIds.get(i), sessions.get(i));
        }
    }

    @Test
    @DisplayName("load_300Sessions_fanoutCompletes — 300 连接注册→单事件推送→全部收到，p95≤5s，无降级，双清理")
    void load_300Sessions_fanoutCompletes() {
        // ── Arrange: 注册 300 用户各 1 mock Session ──
        for (int i = 0; i < SESSION_COUNT; i++) {
            Integer userId = BASE_USER_ID + i;
            Session session = mock(Session.class);
            when(session.isOpen()).thenReturn(true);
            RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
            when(session.getAsyncRemote()).thenReturn(async);

            registry.register(userId, session);
            userIds.add(userId);
            sessions.add(session);
            asyncRemotes.add(async);
        }

        // 验证全部在线
        for (Integer uid : userIds) {
            assertThat(registry.isOnline(uid)).isTrue();
        }

        // ── Act: 逐用户推送，逐调用计时 ──
        long[] latencies = new long[SESSION_COUNT];
        for (int i = 0; i < SESSION_COUNT; i++) {
            long t0 = System.nanoTime();
            registry.pushToUser(userIds.get(i), PAYLOAD);
            latencies[i] = System.nanoTime() - t0;
        }

        // ── Assert 1: 300 remote 各收到 sendText(payload) 恰好 1 次 ──
        for (int i = 0; i < SESSION_COUNT; i++) {
            verify(asyncRemotes.get(i), times(1)).sendText(PAYLOAD);
        }

        // ── Assert 2: p95 耗时 ≤ 5s ──
        long[] sorted = latencies.clone();
        Arrays.sort(sorted);
        long p95 = sorted[284]; // ceil(300*0.95)-1 = 284
        System.out.println("[T035] p95 扇出耗时: " + p95 + " ns (" + (p95 / 1_000_000) + " ms)");
        System.out.println("[T035] max 扇出耗时: " + sorted[299] + " ns (" + (sorted[299] / 1_000_000) + " ms)");
        System.out.println("[T035] min 扇出耗时: " + sorted[0] + " ns (" + (sorted[0] / 1_000_000) + " ms)");
        assertThat(p95)
                .as("p95 扇出耗时应 ≤ 5s")
                .isLessThanOrEqualTo(P95_LIMIT_NS);

        // ── Assert 3: 推送后无连接降级，全部仍在线 ──
        for (Integer uid : userIds) {
            assertThat(registry.isOnline(uid))
                    .as("userId %d 推送后应仍在线", uid)
                    .isTrue();
        }

        // ── Assert 4: 原子双清理 — 逐用户 unregister 后全部离线 ──
        for (int i = 0; i < SESSION_COUNT; i++) {
            registry.unregister(userIds.get(i), sessions.get(i));
        }
        for (Integer uid : userIds) {
            assertThat(registry.isOnline(uid))
                    .as("userId %d unregister 后应离线（原子双清理）", uid)
                    .isFalse();
        }

        // 标记已清理，防止 @AfterEach 重复 unregister（幂等但冗余）
        userIds.clear();
        sessions.clear();
    }
}
