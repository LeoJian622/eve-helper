package xyz.foolcat.eve.evehelper.infrastructure.external.esi.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ForceRefreshLimiter 单元测试(TDD 红-绿)。
 *
 * <p>覆盖 spec FR-007 固定时间窗限流语义:</p>
 * <ul>
 *     <li>窗口内前 N 次通过、第 N+1 次拒</li>
 *     <li>窗口起点用 setIfAbsent 起 1</li>
 *     <li>窗口过期后计数归零重新允许</li>
 * </ul>
 */
@DisplayName("ForceRefreshLimiter 固定时间窗强刷限流")
class ForceRefreshLimiterTest {

    private CacheGateway cacheGateway;
    private ForceRefreshLimiter limiter;

    private static final Duration WINDOW = Duration.ofMinutes(60);
    private static final String KEY = "esi:resp:serenity:1:/x:0";

    @BeforeEach
    void setUp() {
        cacheGateway = mock(CacheGateway.class);
        limiter = new ForceRefreshLimiter(cacheGateway);
    }

    private String windowKey() {
        return KEY + ":frwindow";
    }

    @Test
    @DisplayName("窗口内前 5 次通过,第 6 次拒")
    void withinWindow_firstLimitPass_sixthRejected() {
        // 首调用:窗口不存在 → setIfAbsent 创建窗口返回 TRUE,第 1 次通过
        when(cacheGateway.setIfAbsent(eq(windowKey()), eq(1), longThat(l -> l > 0), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(Boolean.TRUE);
        assertThat(limiter.tryAcquire(KEY, WINDOW, 5)).isTrue();

        // 第 2~5 次:窗口已存在 → setIfAbsent 返回 FALSE(未创建),increment 计数 2..5 均通过
        when(cacheGateway.setIfAbsent(eq(windowKey()), eq(1), longThat(l -> l > 0), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(Boolean.FALSE);
        for (int i = 2; i <= 5; i++) {
            when(cacheGateway.get(windowKey())).thenReturn((long) i - 1);
            when(cacheGateway.increment(windowKey())).thenReturn((long) i);
            assertThat(limiter.tryAcquire(KEY, WINDOW, 5)).isTrue();
        }

        // 第 6 次:窗口已存在 → increment 6 > 5,拒绝
        when(cacheGateway.get(windowKey())).thenReturn(5L);
        when(cacheGateway.increment(windowKey())).thenReturn(6L);
        assertThat(limiter.tryAcquire(KEY, WINDOW, 5)).isFalse();
    }

    @Test
    @DisplayName("首次调用经 setIfAbsent 起一次性计数")
    void firstCall_usesSetIfAbsentOnce() {
        when(cacheGateway.setIfAbsent(eq(windowKey()), eq(1), longThat(l -> l > 0), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(Boolean.TRUE);
        limiter.tryAcquire(KEY, WINDOW, 5);
        verify(cacheGateway, times(1)).setIfAbsent(eq(windowKey()), eq(1), longThat(l -> l > 0), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("窗口过期(键不存在)后计数归零、重新允许")
    void windowExpired_resetsAndAllows() {
        when(cacheGateway.get(windowKey())).thenReturn(null);
        when(cacheGateway.setIfAbsent(eq(windowKey()), eq(1), longThat(l -> l > 0), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(Boolean.TRUE);
        assertThat(limiter.tryAcquire(KEY, WINDOW, 5)).isTrue();
    }
}