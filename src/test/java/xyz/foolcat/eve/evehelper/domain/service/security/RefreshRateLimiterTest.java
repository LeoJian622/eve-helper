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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 *       超阈值 → 告警计数器递增,<b>但不在请求线程上施加任何时延</b>(T036 推翻 v3 的
 *       「固定延迟 100~300ms」——`Thread.sleep` 占住 Tomcat 工作线程,而该端点未认证
 *       可达,反成 DoS 放大器);<b>断言非硬拒</b>(不抛异常、不返回拒绝标志 —— 503 方案
 *       已因与轮换窗口 SC-006 冲突被否决,见 plan §7)</li>
 *   <li><b>成功路径零影响</b>:本服务仅由失败路径调用(T017 接线保证),
 *       且任何路径都不阻塞调用线程</li>
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

    /** 「不施加时延」路径的耗时上限:纯内存操作 + mock,80ms 已留足抖动余量 */
    private static final long NO_DELAY_MAX_MS = 80;

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
    // L2:全局洪泛观测(纯告警,既不硬拒也不施加时延)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("L2 低于阈值:不施加时延、正常返回(成功路径零影响)")
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
    @DisplayName("L2 超阈值:不得阻塞调用线程(T036 防 HIGH-1 回归)且仍非硬拒")
    void l2_overThreshold_doesNotBlockCallingThread() {
        // Arrange
        long overThreshold = RefreshRateLimiterService.L2_THRESHOLD + 1L;
        when(cacheGateway.increment(RefreshRateLimiterService.L2_GLOBAL_KEY)).thenReturn(overThreshold);
        lenient().when(cacheGateway.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        long start = System.currentTimeMillis();
        // 非硬拒判据①:方法正常返回(void,无拒绝标志),调用方仍返回业务错误而非 503
        assertDoesNotThrow(() -> refreshRateLimiterService.observeInvalidRefresh());
        long elapsed = System.currentTimeMillis() - start;

        // Assert:超阈值路径耗时须与低于阈值同量级 —— 「不占用调用线程」的操作性定义。
        // 曾经的 `elapsed >= 100ms`(v3 固定延迟)是 DoS 放大器:Thread.sleep 占住 Tomcat
        // 工作线程,而 POST:/auth/tokens 未认证可达,200 线程下约 1000 req/s 即拖垮全站。
        // 该断言方向已反转,恢复 sleep 会立即 RED(plan v4 §3.3 变异测试第 6 条)。
        assertTrue(elapsed < NO_DELAY_MAX_MS,
                "超阈值不得在请求线程上施加任何时延(DoS 放大器),实际耗时 " + elapsed + "ms");
    }

    @Test
    @DisplayName("L2 延迟常量已移除:防「速率整形」设计回退(T036 结构性断言)")
    void l2_noDelayConstantsRemain() {
        // Assert:v3 的 MIN_DELAY_MS / MAX_DELAY_MS 必须已被删除 —— 常量若还在,
        // 说明延迟逻辑可能被恢复。计时断言易受调度抖动干扰(慢机上 sleep 100ms
        // 仍可能被误判通过),故补一条不依赖计时的结构性断言。
        boolean hasDelayConstant = java.util.Arrays
                .stream(RefreshRateLimiterService.class.getDeclaredFields())
                .anyMatch(f -> f.getName().contains("DELAY"));

        assertFalse(hasDelayConstant,
                "延迟整形方案已于 T036 推翻,不得保留 *DELAY* 常量(防设计回退)");
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
