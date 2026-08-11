package xyz.foolcat.eve.evehelper.domain.service.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * refresh 端点两层限流(007 CRITICAL-2,T016;plan v3 §3.3)。
 *
 * <p><b>为什么是两层、且都不做锁定/硬拒</b>:refresh 的主攻击向量是
 * <b>随机 UUID 洪泛</b> —— 这些请求在 {@code AuthApplicationService.refreshToken}
 * 的 Redis 存在性校验阶段就被挡下,<b>解析不出 userId</b>,因此「按 userId 锁定」
 * 根本看不到主攻击向量,只会误伤真实用户。故:</p>
 * <ul>
 *   <li><b>L1 按 userId 观测</b>(键 {@code refresh:fail:{userId}},TTL 60s):
 *       仅计数 + 告警,<b>禁止任何锁定动作</b> —— 捕捉「针对单一账号的定向刷失败」</li>
 *   <li><b>L2 全局单键观测</b>(键 {@code refresh:invalid:global},固定窗口 60s):
 *       捕捉随机 UUID 洪泛。超阈值 → Prometheus 计数器告警 + 失败路径
 *       <b>固定延迟 100~300ms</b>(速率整形,抬高攻击成本)。
 *       <b>绝不硬拒</b>:503 方案会误伤密钥轮换窗口的合法 refresh(SC-006),已否决</li>
 * </ul>
 *
 * <p><b>fail-open</b>:限流是观测与整形,不是门禁。Redis 故障时静默降级,
 * 不得让 refresh 业务新增失败。</p>
 *
 * <p><b>成功路径零影响</b>:本服务仅由失败路径调用(T017 接线),成功刷新不经过。</p>
 *
 * @author Leojan
 * date 2026-08-12
 */
@Slf4j
@Service
public class RefreshRateLimiterService {

    /** L1 键前缀:每个 userId 一个观测计数器 */
    public static final String L1_KEY_PREFIX = "refresh:fail:";

    /** L2 键:全局单一洪泛观测计数器(不依赖 IP) */
    public static final String L2_GLOBAL_KEY = "refresh:invalid:global";

    /** L1/L2 观测窗口(秒) */
    public static final long OBSERVE_WINDOW_SECONDS = 60L;

    /**
     * L2 洪泛告警阈值(次/窗口)。
     * 取远高于合法峰值的保守值:密钥轮换时全体在线客户端各 refresh 一次,
     * 对本项目用户规模而言远低于该阈值;洪泛攻击则会在数秒内击穿。
     * 阈值集中定义于此(T017 AC),不得散落硬编码。
     */
    public static final long L2_THRESHOLD = 1000L;

    /** 超阈值固定延迟区间(毫秒):速率整形,非惩罚性拒绝 */
    public static final long MIN_DELAY_MS = 100L;
    public static final long MAX_DELAY_MS = 300L;

    private final CacheGateway cacheGateway;

    /** 洪泛告警计数器(超阈值时递增,由 Prometheus 抓取触发告警) */
    private final Counter floodAlertCounter;

    public RefreshRateLimiterService(CacheGateway cacheGateway, MeterRegistry meterRegistry) {
        this.cacheGateway = cacheGateway;
        this.floodAlertCounter = Counter
                .builder("eve.helper.refresh.invalid.flood")
                .description("refresh 无效请求洪泛告警计数(007 CRITICAL-2 L2 超阈值次数)")
                .register(meterRegistry);
    }

    /**
     * L1:记录某用户的 refresh 失败(仅观测计数 + 告警,<b>无任何锁定动作</b>)。
     *
     * @param userId 用户 ID(仅在 Redis 存在性校验之后才能获得,故只覆盖定向攻击向量)
     */
    public void recordUserFailure(Integer userId) {
        try {
            String key = L1_KEY_PREFIX + userId;
            cacheGateway.increment(key);
            cacheGateway.expire(key, OBSERVE_WINDOW_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // fail-open:观测失败不得影响业务
            log.warn("L1 refresh 失败计数异常,fail-open: {}", e.getMessage());
        }
    }

    /**
     * L2:记录一次无效 refresh(全局洪泛观测)。
     *
     * <p>固定窗口:每次 INCR 后重设 EXPIRE。超阈值时递增 Prometheus 计数器
     * 并在当前(失败路径)线程施加 100~300ms 固定延迟 —— 方法本身正常返回,
     * 调用方仍向客户端返回业务错误,<b>不做硬拒</b>。</p>
     */
    public void observeInvalidRefresh() {
        final Long count;
        try {
            count = cacheGateway.increment(L2_GLOBAL_KEY);
            cacheGateway.expire(L2_GLOBAL_KEY, OBSERVE_WINDOW_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // fail-open:Redis 故障不施加延迟、不影响业务
            log.warn("L2 refresh 洪泛计数异常,fail-open: {}", e.getMessage());
            return;
        }

        if (count != null && count > L2_THRESHOLD) {
            floodAlertCounter.increment();
            log.warn("refresh 无效请求洪泛告警: count={}, threshold={}", count, L2_THRESHOLD);
            applyFixedDelay();
        }
    }

    /**
     * 固定延迟整形:区间内取随机值,避免被限速客户端形成同步重试风暴。
     */
    private void applyFixedDelay() {
        long delayMs = ThreadLocalRandom.current().nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
