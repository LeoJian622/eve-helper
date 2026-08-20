package xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 强制刷新固定时间窗限流哨兵。
 * <p>
 * 每键每固定时间窗(如 60 分钟)内允许最多 {@code limit} 次真实刷新，超出拒绝。
 * 窗口以首次真实刷新写入缓存的时间为起点；窗口过期(TTL 到期、计数键自然消失)后计数归零重新允许。
 * <p>
 * 实现：使用 {@link CacheGateway#increment} 自增计数 + {@link CacheGateway#setIfAbsent}
 * 原子起窗(避免并发穿透 double-count)，并以 TTL 保证窗口有界。
 *
 * @author Leojan
 */
@Component
@RequiredArgsConstructor
public class ForceRefreshLimiter {

    private static final String WINDOW_SUFFIX = ":frwindow";

    private final CacheGateway cacheGateway;

    /**
     * 尝试获取一次强刷额度。
     *
     * @return true 允许真实刷新；false 窗口内额度已用尽(拒绝,应由调用方返回现有缓存)
     */
    public boolean tryAcquire(String cacheKey, Duration window, int limit) {
        String windowKey = cacheKey + WINDOW_SUFFIX;
        long windowMillis = window.toMillis();

        // 首次:窗口不存在 → setIfAbsent 起 1 并占窗。起窗成功即视为第 1 次已用掉,返回 true。
        Boolean created = cacheGateway.setIfAbsent(windowKey, 1, windowMillis, TimeUnit.MILLISECONDS);
        if (Boolean.TRUE.equals(created)) {
            return true;
        }

        // 窗口已存在:读计数;竞态下键刚过期读到 null → 重试起窗一次
        Object current = cacheGateway.get(windowKey);
        if (current == null) {
            Boolean retry = cacheGateway.setIfAbsent(windowKey, 1, windowMillis, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(retry);
        }

        // 计数 +1(窗口 TTL 已由首次 setIfAbsent 设定)
        Long refreshed = cacheGateway.increment(windowKey);
        if (refreshed == null || refreshed <= 0) {
            return false;
        }
        // 窗口若非整窗存活(无 TTL),补设 TTL 防止计数键永驻
        Long ttl = cacheGateway.getExpire(windowKey, TimeUnit.MILLISECONDS);
        if (ttl != null && ttl < 0) {
            cacheGateway.expire(windowKey, windowMillis, TimeUnit.MILLISECONDS);
        }
        return refreshed <= limit;
    }
}