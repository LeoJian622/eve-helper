package xyz.foolcat.eve.evehelper.infrastructure.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 线程池身份隔离端到端回归(008 T006 / SC-002)。
 *
 * <p>验证标记 {@code SecurityContextTaskDecorator} 后:
 * <ul>
 *   <li>复用工作线程执行新任务前不携带先前任务残留身份(SC-002);</li>
 *   <li>提交线程有身份时快照显式传播给工作线程(需身份任务);</li>
 *   <li>同线程(CallerRuns)直通不破坏请求线程自身身份(防 {@code AccessGuard} fail-closed)。</li>
 * </ul>
 * 不依赖 Spring 上下文与 DB,直接以真实 {@link ThreadPoolTaskExecutor} 驱动装饰器通路。
 */
class ThreadPoolIdentityIsolationTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static String readAuthenticationName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "null" : auth.getName();
    }

    private static void setAuthenticated(String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void reusedWorkerThreadIsCleanForNextTask() throws Exception {
        // SC-002 核心:复用同一工作线程,任务 A 结束后身份清空,任务 B 读到空身份
        ThreadPoolTaskExecutor single = new ThreadPoolTaskExecutor();
        single.setCorePoolSize(1);
        single.setMaxPoolSize(1);
        single.setQueueCapacity(10);
        single.setTaskDecorator(new SecurityContextTaskDecorator());
        single.initialize();
        try {
            CountDownLatch aDone = new CountDownLatch(1);
            single.execute(() -> {
                setAuthenticated("alice");
                aDone.countDown();
            });
            aDone.await(5, TimeUnit.SECONDS);

            AtomicReference<String> seenB = new AtomicReference<>();
            CountDownLatch bDone = new CountDownLatch(1);
            single.execute(() -> {
                seenB.set(readAuthenticationName());
                bDone.countDown();
            });
            bDone.await(5, TimeUnit.SECONDS);

            assertEquals("null", seenB.get(), "复用工作线程执行新任务前必须清空残留身份");
        } finally {
            single.shutdown();
        }
    }

    @Test
    void submittingThreadIdentityIsPropagatedToTask() throws Exception {
        // 提交线程有身份 -> 快照传播给工作线程(需身份任务的正确传播,非残留)
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) new AsyncConfiguration().esiAuthStatusExecutor();
        try {
            setAuthenticated("alice");
            AtomicReference<String> seen = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            pool.execute(() -> {
                seen.set(readAuthenticationName());
                done.countDown();
            });
            done.await(5, TimeUnit.SECONDS);
            assertEquals("alice", seen.get(), "提交线程身份应快照传播给工作线程");
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void callerRunsDirectPassThroughPreservesRequestThreadIdentity() {
        // CallerRuns 回退:提交线程 == 执行线程时,装饰器同线程直通,不抹掉请求线程自身身份
        setAuthenticated("requser");
        Runnable wrapped = new SecurityContextTaskDecorator().decorate(() -> {
            assertEquals("requser", readAuthenticationName(), "CallerRuns 直通时任务应读到请求线程自身身份");
        });

        wrapped.run(); // 主线程执行,模拟 CallerRuns 回退

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "CallerRuns 直通后请求线程身份不得被抹掉");
        assertEquals("requser", readAuthenticationName());
    }
}