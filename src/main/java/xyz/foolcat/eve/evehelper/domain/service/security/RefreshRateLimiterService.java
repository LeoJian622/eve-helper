package xyz.foolcat.eve.evehelper.domain.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

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
 *       捕捉随机 UUID 洪泛。超阈值 → 打一条含 {@link #ALERT_MARKER} 的结构化 WARN 日志,
 *       <b>仅此而已</b>。<b>绝不硬拒</b>:503 方案会误伤密钥轮换窗口的合法 refresh
 *       (SC-006),已否决</li>
 * </ul>
 *
 * <p><b>为何告警走日志而非 Prometheus(T037 修正名实不符)</b>:v3 声称
 * 「Counter 由 Prometheus 抓取触发告警」,实测该通路<b>完全不存在</b> ——
 * 全仓无 {@code micrometer-registry-prometheus}、无 {@code management:} 配置、
 * pom 中三个 {@code io.prometheus} 依赖在 {@code src/main/java} <b>零引用</b>
 * (无 {@code PrometheusRegistry}、无 exporter 启动)。故 {@code MeterRegistry}
 * 只会被 actuator 兜底成 {@code SimpleMeterRegistry}:仅内存、<b>永不被抓取</b>。
 * 持有它等于代码声称有告警能力而实际没有。技术栈已冻结(宪法第四条),新增
 * registry 依赖须走修订程序,而<b>日志是本项目现存唯一可运维的告警通路</b>,
 * 故改为结构化 WARN + 日志告警规则(见 {@code docs/DEPLOYMENT.md}「日志告警」)。</p>
 *
 * <p><b>为何不做延迟整形(T036 推翻 v3 裁决)</b>:v3 曾在超阈值时施加 100~300ms
 * {@code Thread.sleep}「抬高攻击成本」,方向判反了 —— sleep 占住的是 Tomcat 工作线程,
 * 而 {@code POST:/auth/tokens} 已加白(未认证可达)。洪泛用并发连接,延迟不降低攻击者
 * 吞吐,却让默认 200 线程在约 1000 req/s 下耗尽,把单端点压力放大成<b>全站</b>可用性故障
 * (无延迟时需 ~200,000 req/s)。且 FR-015 清空 {@code refresh_token:*} 后全体客户端
 * 同时失败会<b>自我触发</b>该路径。真正的抗洪泛来自失败路径 O(1) 廉价:随机 UUID 在
 * Redis 存在性校验即止,不触 DB、不触 token 生成、不触 RBAC。若确需速率限制,应在
 * <b>入口层</b>(反代/网关/{@code server.tomcat.max-connections})做,而非在业务线程内自我阻塞。</p>
 *
 * <p><b>fail-open</b>:限流是观测,不是门禁。Redis 故障时静默降级,
 * 不得让 refresh 业务新增失败。</p>
 *
 * <p><b>成功路径零影响</b>:本服务仅由失败路径调用(T017 接线),成功刷新不经过;
 * 且任何路径都不阻塞调用线程。</p>
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

    /**
     * 告警日志稳定标记。
     *
     * <p>日志告警规则(Loki/ELK/grep)按此串匹配,故<b>不得随意修改</b> ——
     * 改动等于让既有告警规则静默失效。测试对该常量有断言(T037)。</p>
     */
    public static final String ALERT_MARKER = "[SECURITY_ALERT:REFRESH_FLOOD]";

    private final CacheGateway cacheGateway;

    public RefreshRateLimiterService(CacheGateway cacheGateway) {
        this.cacheGateway = cacheGateway;
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
     * <p>固定窗口:每次 INCR 后重设 EXPIRE。超阈值时递增告警计数器并打 WARN 日志,
     * <b>随即返回 —— 不阻塞调用线程、不硬拒</b>。调用方仍向客户端返回业务错误。</p>
     */
    public void observeInvalidRefresh() {
        final Long count;
        try {
            count = cacheGateway.increment(L2_GLOBAL_KEY);
            cacheGateway.expire(L2_GLOBAL_KEY, OBSERVE_WINDOW_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // fail-open:Redis 故障不影响业务
            log.warn("L2 refresh 洪泛计数异常,fail-open: {}", e.getMessage());
            return;
        }

        if (count != null && count > L2_THRESHOLD) {
            log.warn("{} refresh 无效请求洪泛: count={}, threshold={}, windowSeconds={}",
                    ALERT_MARKER, count, L2_THRESHOLD, OBSERVE_WINDOW_SECONDS);
        }
    }
}
