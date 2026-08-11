package xyz.foolcat.eve.evehelper.domain.service.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * refresh 两层限流契约测试(007 CRITICAL-2,T010;plan v3 §3.3)。
 *
 * <p>设计约束(第三轮评审确认,逐条对应断言):</p>
 * <ul>
 *   <li><b>L1 按 userId 观测</b>(键 {@code refresh:fail:{userId}},TTL 60s):
 *       仅计数告警,<b>禁止任何锁定动作</b>(随机 UUID 洪泛在 userId 解析前就被挡,
 *       L1 看不到主攻击向量,锁定只会误伤)</li>
 *   <li><b>L2 全局单键观测</b>(键 {@code refresh:invalid:global},固定窗口):
 *       超阈值 → Prometheus 计数器告警 + <b>失败路径固定延迟 100~300ms</b>;
 *       <b>断言非硬拒</b>(不抛异常、不返回拒绝标志 —— 503 方案已因与轮换窗口
 *       SC-006 冲突被否决,见 plan §7)</li>
 *   <li><b>成功路径零影响</b>:本服务仅由失败路径调用(T017 接线保证),
 *       且低于阈值时不施加延迟</li>
 *   <li><b>Redis 故障 fail-open</b>:限流是观测与整形,不是门禁;
 *       Redis 异常不得让 refresh 业务新增失败</li>
 * </ul>
 *
 * <p>TDD 状态:T010 = RED({@code RefreshRateLimiterService} 尚不存在,编译失败);
 * T016 实现后转 GREEN。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("refresh 两层限流契约测试(007 CRITICAL-2)")
class RefreshRateLimiterTest {

    /** 无延迟路径的耗时上限(与 100ms 最低延迟之间留足余量,防计时抖动) */
    private static final long NO_DELAY_MAX_MS = 80;

    /** 超阈值固定延迟的最低值(plan §3.3:100~300ms) */
    private static final long MIN_DELAY_MS = 100;

    @Mock
    private CacheGateway cacheGateway;

    private SimpleMeterRegistry meterRegistry;

    private RefreshRateLimiterService refreshRateLimiterService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        refreshRateLimiterService = new RefreshRateLimiterService(cacheGateway, meterRegistry);
    }

    // ------------------------------------------------------------------
    // L2:全局洪泛观测 + 延迟整形(非硬拒)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("L2 低于阈值:无延迟、正常返回(成功路径零影响)")
    void l2_belowThreshold_noDelayNoReject() {
        // Arrange
        when(cacheGateway.increment(RefreshRateLimiterService.L2_GLOBAL_KEY)).thenReturn(1L);
        lenient().when(cacheGateway.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        long start = System.currentTimeMillis();
        assertDoesNotThrow(() -> refreshRateLimiterService.observeInvalidRefresh());
        long elapsed = System.currentTimeMillis() - start;

        // Assert:低于阈值不施加延迟
        assertTrue(elapsed < NO_DELAY_MAX_MS,
                "低于阈值不应延迟,实际耗时 " + elapsed + "ms");
    }

    @Test
    @DisplayName("L2 超阈值:固定延迟 ≥100ms 且仍非硬拒(断言不抛异常、无拒绝标志)")
    void l2_overThreshold_fixedDelayButNotHardReject() {
        // Arrange
        long overThreshold = RefreshRateLimiterService.L2_THRESHOLD + 1L;
        when(cacheGateway.increment(RefreshRateLimiterService.L2_GLOBAL_KEY)).thenReturn(overThreshold);
        lenient().when(cacheGateway.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        long start = System.currentTimeMillis();
        // 非硬拒判据①:方法正常返回(void,无拒绝标志),调用方仍返回业务错误而非 503
        assertDoesNotThrow(() -> refreshRateLimiterService.observeInvalidRefresh());
        long elapsed = System.currentTimeMillis() - start;

        // Assert:固定延迟生效(100~300ms 下限断言;上限留宽松余量防调度抖动)
        assertTrue(elapsed >= MIN_DELAY_MS,
                "超阈值应施加固定延迟 ≥" + MIN_DELAY_MS + "ms,实际 " + elapsed + "ms");
        assertTrue(elapsed < 2000, "延迟不应超出整形目的(100~300ms)过多,实际 " + elapsed + "ms");
    }

    @Test
    @DisplayName("L2 超阈值:Prometheus 计数器递增(告警通路)")
    void l2_overThreshold_prometheusCounterIncrements() {
        // Arrange
        long overThreshold = RefreshRateLimiterService.L2_THRESHOLD + 1L;
        when(cacheGateway.increment(RefreshRateLimiterService.L2_GLOBAL_KEY)).thenReturn(overThreshold);
        lenient().when(cacheGateway.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        double before = sumAllCounters();

        // Act
        refreshRateLimiterService.observeInvalidRefresh();

        // Assert:至少一个计数器递增(名称由实现定,行为契约是「有告警信号」)
        assertTrue(sumAllCounters() > before, "超阈值应触发 Prometheus 计数器告警");
    }

    @Test
    @DisplayName("L2 Redis 故障:fail-open(不抛异常、不延迟、不新增业务失败)")
    void l2_redisFailure_failOpen() {
        // Arrange
        when(cacheGateway.increment(RefreshRateLimiterService.L2_GLOBAL_KEY))
                .thenThrow(new RuntimeException("Redis down"));

        // Act & Assert
        long start = System.currentTimeMillis();
        assertDoesNotThrow(() -> refreshRateLimiterService.observeInvalidRefresh(),
                "Redis 故障必须 fail-open,不得让 refresh 业务新增失败");
        assertTrue(System.currentTimeMillis() - start < NO_DELAY_MAX_MS,
                "Redis 故障时不应施加延迟");
    }

    // ------------------------------------------------------------------
    // L1:按 userId 观测计数(仅告警,禁止锁定)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("L1 记录失败计数:仅 increment + TTL 60s,无任何锁定动作")
    void l1_recordsCountOnly_neverLocks() {
        // Arrange
        when(cacheGateway.increment("refresh:fail:42")).thenReturn(3L);
        when(cacheGateway.expire(eq("refresh:fail:42"), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // Act
        assertDoesNotThrow(() -> refreshRateLimiterService.recordUserFailure(42));

        // Assert:除计数与 TTL 外无任何网关操作 —— 「无锁定动作」的操作性定义
        verify(cacheGateway).increment("refresh:fail:42");
        verify(cacheGateway).expire("refresh:fail:42", 60L, TimeUnit.SECONDS);
        verifyNoMoreInteractions(cacheGateway);
    }

    @Test
    @DisplayName("L1 Redis 故障:fail-open(不抛异常)")
    void l1_redisFailure_failOpen() {
        // Arrange
        when(cacheGateway.increment(anyString())).thenThrow(new RuntimeException("Redis down"));

        // Act & Assert
        assertDoesNotThrow(() -> refreshRateLimiterService.recordUserFailure(42),
                "L1 是观测计数,Redis 故障不得影响业务");
    }

    /** 汇总 registry 中全部计数器当前值(不固化指标名,只断言「有告警信号」) */
    private double sumAllCounters() {
        return meterRegistry.getMeters().stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
    }
}
