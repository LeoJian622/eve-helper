package xyz.foolcat.eve.evehelper.infrastructure.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SecurityContextTaskDecorator 单元测试(R1 五点语义)
 *
 * @see SecurityContextTaskDecorator
 */
class SecurityContextTaskDecoratorTest {

    private final SecurityContextTaskDecorator decorator = new SecurityContextTaskDecorator();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static SecurityContext authenticatedContext(String username) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        return ctx;
    }

    @Test
    void propagatesSnapshotToWorkerThread() throws Exception {
        // ① 提交线程有身份 -> 工作线程读到同一身份(快照传播)
        SecurityContextHolder.setContext(authenticatedContext("alice"));
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable wrapped = decorator.decorate(() -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            seen.set(auth == null ? "null" : auth.getName());
        });

        Thread worker = new Thread(wrapped);
        worker.start();
        worker.join();

        assertEquals("alice", seen.get());
    }

    @Test
    void workerThreadClearsPriorIdentityBeforeExecution() throws Exception {
        // ② 执行前 clearContext:工作线程的残留身份必须在执行前被清除
        AtomicReference<String> seen = new AtomicReference<>();
        // 主线程(提交线程)无身份 -> 快照为 null
        Runnable wrapped = decorator.decorate(() -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            seen.set(auth == null ? "null" : auth.getName());
        });

        Thread worker = new Thread(() -> {
            // 模拟工作线程上次任务残留的身份
            SecurityContextHolder.setContext(authenticatedContext("prevuser"));
            wrapped.run();
        });
        worker.start();
        worker.join();

        assertEquals("null", seen.get(), "执行前必须清除工作线程残留身份");
    }

    @Test
    void clearsContextAfterTaskOnWorkerThread() throws Exception {
        // ② 执行后 finally clearContext:任务结束后工作线程身份恒空
        AtomicReference<String> afterSeen = new AtomicReference<>();
        Runnable wrapped = decorator.decorate(() -> {
            // 任务执行中产生身份
            SecurityContextHolder.setContext(authenticatedContext("taskuser"));
        });

        Thread worker = new Thread(() -> {
            wrapped.run();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            afterSeen.set(auth == null ? "null" : auth.getName());
        });
        worker.start();
        worker.join();

        assertEquals("null", afterSeen.get(), "任务结束后必须清理身份");
    }

    @Test
    void sameThreadDirectPassThroughDoesNotTouchContext() {
        // ③ 同线程直通:提交线程 == 执行线程(CallerRunsPolicy)时不触碰上下文
        SecurityContextHolder.setContext(authenticatedContext("requser"));
        Runnable wrapped = decorator.decorate(() -> {
            // 任务体不触碰上下文
        });

        wrapped.run(); // 主线程自己执行,模拟 CallerRuns 回退

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "同线程直通不得抹掉请求线程自身身份");
        assertEquals("requser", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void nullSnapshotRunsAnonymousOnWorkerThread() throws Exception {
        // ④ 快照为 null(匿名/调度线程提交)-> 任务匿名执行
        SecurityContextHolder.clearContext(); // 提交线程无身份
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable wrapped = decorator.decorate(() -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            seen.set(auth == null ? "null" : auth.getName());
        });

        Thread worker = new Thread(wrapped);
        worker.start();
        worker.join();

        assertEquals("null", seen.get());
    }

    @Test
    void snapshotIsolationFromSubmittingThreadLateChange() throws Exception {
        // ⑤ 快照为 createEmptyContext() 副本,提交线程后续变更不被工作线程观察
        SecurityContextHolder.setContext(authenticatedContext("alice"));
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable wrapped = decorator.decorate(() -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            seen.set(auth == null ? "null" : auth.getName());
        });

        // 提交线程随后清空自己上下文(模拟请求结束)
        SecurityContextHolder.clearContext();

        Thread worker = new Thread(wrapped);
        worker.start();
        worker.join();

        assertEquals("alice", seen.get(), "工作线程必须读到提交时的快照,而非提交线程后续状态");
    }
}