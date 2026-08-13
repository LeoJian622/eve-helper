package xyz.foolcat.eve.evehelper.domain.service.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.util.List;
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
 *       超阈值 → <b>结构化 WARN 日志告警</b>(T037:原「Prometheus 计数器」通路
 *       实际不存在,详见 {@code RefreshRateLimiterService} 类注释),
 *       <b>但不在请求线程上施加任何时延</b>(T036 推翻 v3 的「固定延迟 100~300ms」
 *       ——`Thread.sleep` 占住 Tomcat 工作线程,而该端点未认证可达,反成 DoS 放大器);
 *       <b>断言非硬拒</b>(不抛异常、不返回拒绝标志 —— 503 方案
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
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("refresh 两层限流契约测试(007 CRITICAL-2)")
class RefreshRateLimiterTest {

    /** 「不施加时延」路径的耗时上限:纯内存操作 + mock,80ms 已留足抖动余量 */
    private static final long NO_DELAY_MAX_MS = 80;

    @MockBean
    private CacheGateway cacheGateway;

    @Autowired
    private RefreshRateLimiterService refreshRateLimiterService;

    /** 捕获被测类日志的 appender —— 告警通路即日志,故须能断言日志内容 */
    private ListAppender<ILoggingEvent> listAppender;

    private List<ILoggingEvent> logEvents;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(RefreshRateLimiterService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logEvents = listAppender.list;
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(RefreshRateLimiterService.class);
        logger.detachAppender(listAppender);
        listAppender.stop();
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
    @DisplayName("L2 超阈值:发出可被日志告警规则匹配的结构化 WARN(T037 告警通路)")
    void l2_overThreshold_emitsStructuredWarnForAlerting() {
        // Arrange
        long overThreshold = RefreshRateLimiterService.L2_THRESHOLD + 1L;
        when(cacheGateway.increment(RefreshRateLimiterService.L2_GLOBAL_KEY)).thenReturn(overThreshold);
        lenient().when(cacheGateway.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        refreshRateLimiterService.observeInvalidRefresh();

        // Assert:告警通路的操作性定义 —— 日志中必须出现稳定可 grep 的告警标记。
        // T037:原断言「Prometheus 计数器递增」立论不成立 —— 全仓无
        // micrometer-registry-prometheus、无 management: 配置、pom 的三个 io.prometheus
        // 依赖在 src/main/java 零引用 ⇒ Counter 落进 SimpleMeterRegistry 永不被抓取。
        // 改为断言日志,因为日志是本项目**实际存在**的唯一可运维告警通路。
        List<ILoggingEvent> warns = logEvents.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .toList();
        assertFalse(warns.isEmpty(), "超阈值必须发出 WARN 级日志");

        String formatted = warns.get(warns.size() - 1).getFormattedMessage();
        assertTrue(formatted.contains(RefreshRateLimiterService.ALERT_MARKER),
                "告警日志须含稳定标记 " + RefreshRateLimiterService.ALERT_MARKER
                        + "(供 alerts 规则 grep),实际: " + formatted);
        assertTrue(formatted.contains(String.valueOf(overThreshold)),
                "告警日志须含实际计数值以便定量判断,实际: " + formatted);
        assertTrue(formatted.contains(String.valueOf(RefreshRateLimiterService.L2_THRESHOLD)),
                "告警日志须含阈值以便判断超出幅度,实际: " + formatted);
    }

    @Test
    @DisplayName("L2 低于阈值:不发告警(避免告警疲劳)")
    void l2_belowThreshold_noAlert() {
        // Arrange
        when(cacheGateway.increment(RefreshRateLimiterService.L2_GLOBAL_KEY)).thenReturn(1L);
        lenient().when(cacheGateway.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        refreshRateLimiterService.observeInvalidRefresh();

        // Assert:未超阈值不得出现告警标记 —— 否则告警规则会被正常流量刷爆
        boolean anyAlert = logEvents.stream()
                .anyMatch(e -> e.getFormattedMessage().contains(RefreshRateLimiterService.ALERT_MARKER));
        assertFalse(anyAlert, "低于阈值不应发出告警标记");
    }

    @Test
    @DisplayName("告警通路名实一致:不得残留 MeterRegistry 依赖(T037 防名实不符回退)")
    void noUnreachableMetricsDependency() {
        // Assert:项目无 micrometer-registry-prometheus、无 management: 配置,
        // MeterRegistry 只会被 actuator 兜底成 SimpleMeterRegistry(仅内存,永不导出)。
        // 保留它 = 代码声称有告警能力而实际没有,这正是 T037 判 BLOCK 的主因。
        // 若日后真建成 Prometheus 抓取通路,应删除本用例并同步 DEPLOYMENT.md。
        boolean hasMeterRegistry = java.util.Arrays
                .stream(RefreshRateLimiterService.class.getDeclaredFields())
                .anyMatch(f -> f.getType().getName().contains("micrometer"));

        assertFalse(hasMeterRegistry,
                "无可达的 metrics 导出通路时不得持有 MeterRegistry(名实不符);"
                        + "告警走结构化日志,见 DEPLOYMENT.md「日志告警」节");
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

    // ==================================================================
    // T046:L1 定向攻击告警(阈值 10 次/分钟,用户 2026-08-12 决策)
    //
    // 修正 T037 遗留:L1 此前只写不读 —— 计数器递增却无任何消费方,
    // 针对单一账号的定向刷失败无法触发告警,是纯哑计数器。
    // ==================================================================

    @Test
    @DisplayName("T046:L1 超 10 次/分钟 → 打含定向攻击标记的 WARN(含 userId 供运维定位)")
    void l1_overThreshold_emitsTargetedAlert() {
        // Arrange:第 11 次失败(阈值 10)
        when(cacheGateway.increment("refresh:fail:42")).thenReturn(11L);
        when(cacheGateway.expire(eq("refresh:fail:42"), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // Act
        refreshRateLimiterService.recordUserFailure(42);

        // Assert
        List<ILoggingEvent> warns = listAppender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .toList();
        assertFalse(warns.isEmpty(), "L1 超阈值必须打 WARN 告警,否则定向攻击无法察觉");

        String msg = warns.get(0).getFormattedMessage();
        assertTrue(msg.contains(RefreshRateLimiterService.ALERT_MARKER_TARGETED),
                "告警须含定向攻击稳定标记(供 Loki/grep 规则匹配),实际: " + msg);
        assertTrue(msg.contains("42"),
                "告警须含 userId —— 否则运维无法定位被攻击账号,实际: " + msg);
        assertTrue(msg.contains("11"),
                "告警须含实际计数,实际: " + msg);
    }

    @Test
    @DisplayName("T046:L1 恰好 10 次 → 不告警(阈值语义为「超过」,避免边界抖动误报)")
    void l1_atThreshold_noAlert() {
        // Arrange
        when(cacheGateway.increment("refresh:fail:42")).thenReturn(10L);
        when(cacheGateway.expire(eq("refresh:fail:42"), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // Act
        refreshRateLimiterService.recordUserFailure(42);

        // Assert
        boolean hasTargetedAlert = listAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage()
                        .contains(RefreshRateLimiterService.ALERT_MARKER_TARGETED));
        assertFalse(hasTargetedAlert, "恰好达到阈值不应告警(阈值语义:严格大于)");
    }

    @Test
    @DisplayName("T046:L1 告警标记与 L2 不同 —— 两类攻击须可分别配置告警规则")
    void l1_alertMarkerDistinctFromL2() {
        assertFalse(RefreshRateLimiterService.ALERT_MARKER_TARGETED
                        .equals(RefreshRateLimiterService.ALERT_MARKER),
                "定向攻击(L1)与洪泛(L2)是不同事件、响应动作不同(封账号 vs 入口限流),"
                        + "共用标记会让运维无法区分");
    }

    @Test
    @DisplayName("T046:L1 超阈值仍不锁定 —— 只多一条日志,不新增任何网关操作")
    void l1_overThreshold_stillNeverLocks() {
        // Arrange
        when(cacheGateway.increment("refresh:fail:42")).thenReturn(999L);
        when(cacheGateway.expire(eq("refresh:fail:42"), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // Act
        assertDoesNotThrow(() -> refreshRateLimiterService.recordUserFailure(42));

        // Assert:告警不得演化成锁定 —— 按 userId 锁定 = 定向锁死 DoS(plan §3.3 明令禁止)
        verify(cacheGateway).increment("refresh:fail:42");
        verify(cacheGateway).expire("refresh:fail:42", 60L, TimeUnit.SECONDS);
        verifyNoMoreInteractions(cacheGateway);
    }

    @Test
    @DisplayName("T046:L1 阈值与窗口来自常量,不得散落硬编码")
    void l1_thresholdIsNamedConstant() {
        assertTrue(RefreshRateLimiterService.L1_THRESHOLD == 10L,
                "L1 阈值应为用户决策的 10 次/分钟,实际: " + RefreshRateLimiterService.L1_THRESHOLD);
        assertTrue(RefreshRateLimiterService.OBSERVE_WINDOW_SECONDS == 60L,
                "「1 分钟」须由 OBSERVE_WINDOW_SECONDS=60 承载,实际: "
                        + RefreshRateLimiterService.OBSERVE_WINDOW_SECONDS);
    }
}
